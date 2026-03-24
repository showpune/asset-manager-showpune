// Azure Database for PostgreSQL Flexible Server module
@description('Location for the PostgreSQL server')
param location string

@description('Name of the PostgreSQL flexible server')
param serverName string

@description('Administrator login username')
param administratorLogin string

@description('Administrator login password')
@secure()
param administratorPassword string

@description('SKU name for the PostgreSQL server')
param skuName string = 'Standard_D2s_v3'

@description('SKU tier for the PostgreSQL server')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param skuTier string = 'GeneralPurpose'

@description('PostgreSQL version')
@allowed(['14', '15', '16'])
param postgresVersion string = '16'

@description('Storage size in GB')
param storageSizeGB int = 32

@description('High availability mode')
@allowed(['Disabled', 'SameZone', 'ZoneRedundant'])
param highAvailabilityMode string = 'Disabled'

@description('Database name')
param databaseName string = 'assets_manager'

@description('Tags to apply to resources')
param tags object = {}

@description('Object ID of the web app managed identity (for AAD admin)')
param webAppObjectId string

@description('Name of the web app managed identity')
param webAppIdentityName string

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: serverName
  location: location
  tags: tags
  sku: {
    name: skuName
    tier: skuTier
  }
  properties: {
    version: postgresVersion
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorPassword
    storage: {
      storageSizeGB: storageSizeGB
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: highAvailabilityMode
    }
    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Enabled'
    }
  }
}

resource postgresDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  parent: postgresServer
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

// Configure Microsoft Entra admin for Managed Identity authentication
resource postgresAadAdmin 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2022-12-01' = {
  parent: postgresServer
  name: webAppObjectId
  properties: {
    principalType: 'ServicePrincipal'
    principalName: webAppIdentityName
    tenantId: subscription().tenantId
  }
}

// Allow Azure services to connect
resource postgresFirewallRuleAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

@description('PostgreSQL server name')
output serverName string = postgresServer.name

@description('PostgreSQL server FQDN')
output serverFqdn string = postgresServer.properties.fullyQualifiedDomainName

@description('Database name')
output databaseName string = postgresDatabase.name

@description('PostgreSQL server resource ID')
output serverId string = postgresServer.id

@description('PostgreSQL JDBC connection string (passwordless with Managed Identity)')
output jdbcConnectionString string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/${databaseName}'
