@description('Azure region for the resource')
param location string

@description('Application name prefix used for resource naming')
param appName string

@description('Deployment environment (dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environment string

@description('PostgreSQL administrator login name')
param adminLogin string

@secure()
@description('PostgreSQL administrator login password')
param adminPassword string

@description('Resource ID of the user-assigned managed identity for AAD authentication')
param managedIdentityId string

@description('Client ID of the managed identity (used in JDBC connection string)')
param managedIdentityClientId string

@description('Principal ID of the managed identity for AAD admin assignment')
param managedIdentityPrincipalId string

@description('Name of the managed identity (used as AAD admin display name)')
param managedIdentityName string

@description('PostgreSQL server compute SKU')
param skuName string = 'Standard_D2ds_v4'

@description('PostgreSQL server compute tier')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param skuTier string = 'GeneralPurpose'

@description('PostgreSQL server storage size in MB')
param storageSizeGB int = 32

@description('PostgreSQL version')
@allowed(['14', '15', '16'])
param postgresVersion string = '16'

@description('Name of the application database')
param databaseName string = 'assets_manager'

@description('Backup retention days')
@minValue(7)
@maxValue(35)
param backupRetentionDays int = 7

@description('Resource tags')
param tags object = {}

var uniqueSuffix = take(uniqueString(resourceGroup().id), 6)
var serverName = 'psql-${appName}-${environment}-${uniqueSuffix}'

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-06-01-preview' = {
  name: serverName
  location: location
  tags: tags
  sku: {
    name: skuName
    tier: skuTier
  }
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${managedIdentityId}': {}
    }
  }
  properties: {
    version: postgresVersion
    administratorLogin: adminLogin
    administratorLoginPassword: adminPassword
    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Enabled'
      tenantId: subscription().tenantId
    }
    storage: {
      storageSizeGB: storageSizeGB
    }
    backup: {
      backupRetentionDays: backupRetentionDays
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    network: {
      publicNetworkAccess: 'Enabled'
    }
  }
}

// Allow all Azure services to connect (adjust with specific IPs for production)
resource allowAzureServicesRule 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2023-06-01-preview' = {
  parent: postgresServer
  name: 'AllowAllAzureServicesAndResourcesWithinAzureIps'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource assetsDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2023-06-01-preview' = {
  parent: postgresServer
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

// Register the managed identity as AAD administrator for the PostgreSQL server
resource aadAdmin 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2023-06-01-preview' = {
  parent: postgresServer
  name: managedIdentityPrincipalId
  properties: {
    principalType: 'ServicePrincipal'
    principalName: managedIdentityName
    tenantId: subscription().tenantId
  }
  dependsOn: []
}

@description('Fully qualified hostname of the PostgreSQL server')
output hostname string = postgresServer.properties.fullyQualifiedDomainName

@description('PostgreSQL server port')
output port int = 5432

@description('Name of the application database')
output databaseName string = assetsDatabase.name

@description('Name of the PostgreSQL flexible server')
output serverName string = postgresServer.name

@description('JDBC connection string template (password-based, for migration phase)')
output jdbcConnectionString string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/${databaseName}?sslmode=require'

@description('JDBC connection string template using Managed Identity (passwordless)')
output jdbcConnectionStringMI string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/${databaseName}?sslmode=require&authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin&azure.clientId=${managedIdentityClientId}'
