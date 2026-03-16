@description('The Azure region to deploy PostgreSQL resources')
param location string

@description('The name of the PostgreSQL flexible server')
param serverName string

@description('Administrator login username for the PostgreSQL server')
param administratorLogin string

@description('Administrator login password for the PostgreSQL server')
@secure()
param administratorLoginPassword string

@description('The name of the initial database to create')
param databaseName string

@description('Principal (object) ID of the managed identity for Azure AD authentication')
param managedIdentityPrincipalId string

@description('Display name of the managed identity for Azure AD admin configuration')
param managedIdentityName string

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: serverName
  location: location
  sku: {
    name: 'Standard_B2ms'
    tier: 'Burstable'
  }
  properties: {
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorLoginPassword
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
    version: '14'
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

// Configure the managed identity as Azure AD administrator for credential-free access
resource postgresAdAdministrator 'Microsoft.DBforPostgreSQL/flexibleServers/administrators@2022-12-01' = {
  parent: postgresServer
  // The resource name must be the AAD object ID (principalId) of the admin
  name: managedIdentityPrincipalId
  properties: {
    principalType: 'ServicePrincipal'
    principalName: managedIdentityName
    tenantId: subscription().tenantId
  }
  dependsOn: [
    postgresDatabase
  ]
}

// Allow Azure services to access the PostgreSQL server
resource firewallAllowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgresServer
  name: 'AllowAllAzureIps'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

@description('Name of the PostgreSQL flexible server')
output serverName string = postgresServer.name

@description('Fully qualified domain name of the PostgreSQL server')
output fullyQualifiedDomainName string = postgresServer.properties.fullyQualifiedDomainName

@description('Resource ID of the PostgreSQL server')
output serverId string = postgresServer.id

@description('Name of the created database')
output databaseName string = postgresDatabase.name
