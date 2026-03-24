// Azure Database for PostgreSQL Flexible Server module
// Provisions a PostgreSQL 14 flexible server with the assets_manager database.
// Both password and Azure AD (managed identity) authentication are enabled,
// allowing the application to migrate to credential-free auth over time.

@description('Azure region for the PostgreSQL flexible server.')
param location string

@description('Name of the PostgreSQL flexible server.')
param serverName string

@description('Administrator login username.')
param administratorLogin string

@description('Administrator login password (stored securely).')
@secure()
param administratorPassword string

@description('Compute SKU name (e.g., Standard_B2s, Standard_D2s_v3).')
param skuName string

@description('Compute tier for the flexible server.')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param skuTier string

@description('Storage size in GB.')
param storageSizeGB int

@description('Resource ID of the user-assigned managed identity for Azure AD authentication.')
param managedIdentityId string

var databaseName = 'assets_manager'

resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: serverName
  location: location
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
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorPassword
    // Enable both password and Azure AD authentication for a smooth migration path
    authConfig: {
      activeDirectoryAuth: 'Enabled'
      passwordAuth: 'Enabled'
      tenantId: subscription().tenantId
    }
    storage: {
      storageSizeGB: storageSizeGB
    }
    version: '14'
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
  }
}

// assets_manager database matching existing application configuration
resource assetsDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  parent: postgresServer
  name: databaseName
  properties: {
    charset: 'UTF8'
    collation: 'en_US.utf8'
  }
}

// Allow connections from other Azure services (App Service, Container Apps, etc.)
resource allowAzureServicesFirewallRule 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

@description('Name of the provisioned PostgreSQL flexible server.')
output serverName string = postgresServer.name

@description('Fully qualified domain name of the PostgreSQL server.')
output fqdn string = postgresServer.properties.fullyQualifiedDomainName

@description('Name of the application database.')
output databaseName string = databaseName
