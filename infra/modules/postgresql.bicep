// Azure Database for PostgreSQL Flexible Server module
// Provisions a PostgreSQL flexible server and a database for the application.
// Configures Microsoft Entra (Azure AD) authentication so the managed identity
// can authenticate without a password.

@description('Azure region for deployment')
param location string

@description('Name of the PostgreSQL flexible server (globally unique)')
param serverName string

@description('Name of the application database')
param databaseName string = 'assetsdb'

@description('Administrator login username (used only for initial setup; prefer MI auth at runtime)')
param adminLogin string

@description('Administrator login password')
@secure()
param adminPassword string

@description('Object ID of the managed identity (used as the Entra admin)')
param managedIdentityObjectId string

@description('Display name of the managed identity (used as the Entra admin login)')
param managedIdentityName string

@description('Client ID of the managed identity (used to configure passwordless JDBC driver)')
param managedIdentityClientId string

@description('Tags to apply to resources')
param tags object = {}

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2024-08-01' = {
  name: serverName
  location: location
  tags: tags
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '16'
    administratorLogin: adminLogin
    administratorLoginPassword: adminPassword
    storage: {
      storageSizeGB: 32
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Enabled'
    }
    network: {}
  }
}

resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2024-08-01' = {
  parent: postgresServer
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.UTF8'
  }
}

// Register the managed identity as the Entra AD administrator
resource entraAdmin 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2024-08-01' = {
  parent: postgresServer
  name: managedIdentityObjectId
  properties: {
    principalType: 'ServicePrincipal'
    principalName: managedIdentityName
    tenantId: tenant().tenantId
  }
  dependsOn: [database]
}

// Allow all Azure services to access the server (tighten in production with VNet rules)
resource azureServicesFirewallRule 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2024-08-01' = {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

@description('Resource ID of the PostgreSQL flexible server')
output serverId string = postgresServer.id

@description('Fully-qualified domain name of the PostgreSQL server')
output serverFqdn string = postgresServer.properties.fullyQualifiedDomainName

@description('Name of the application database')
output databaseName string = database.name

@description('JDBC connection string using the Azure AD passwordless plugin')
output jdbcConnectionString string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/${databaseName}?sslmode=require&authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin&azure.clientId=${managedIdentityClientId}'
