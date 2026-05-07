@description('Location for the PostgreSQL flexible server.')
param location string

@description('Environment name for resource token generation.')
param environmentName string

@description('Administrator login name for the PostgreSQL server.')
param administratorLogin string

@description('Administrator login password for the PostgreSQL server.')
@secure()
param administratorLoginPassword string

@description('Principal ID (object ID) of the managed identity for Entra admin assignment.')
param identityPrincipalId string

@description('Display name of the managed identity.')
param identityName string

var resourceToken = uniqueString(subscription().id, resourceGroup().id, location, environmentName)
var serverName = 'azpg${resourceToken}2'
var databaseName = 'asset_manager'

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-06-01-preview' = {
  name: serverName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorLoginPassword
    version: '17'
    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Enabled'
    }
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
  }
}

// Allow traffic from Azure services
resource firewallAllowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2023-06-01-preview' = {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource postgresDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2023-06-01-preview' = {
  parent: postgresServer
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

// Assign managed identity as Entra administrator
resource postgresEntraAdmin 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2023-06-01-preview' = {
  parent: postgresServer
  name: identityPrincipalId
  properties: {
    principalType: 'ServicePrincipal'
    principalName: identityName
    tenantId: subscription().tenantId
  }
}

output postgresServerName string = postgresServer.name
output postgresServerId string = postgresServer.id
output postgresFqdn string = postgresServer.properties.fullyQualifiedDomainName
output postgresDatabaseName string = postgresDatabase.name
