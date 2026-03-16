@description('The name of the environment (e.g., dev, staging, prod). Used in resource naming.')
@minLength(1)
@maxLength(10)
param environmentName string = 'dev'

@description('The Azure region where all resources will be deployed.')
param location string = resourceGroup().location

@description('Short project name prefix used in resource naming (lowercase alphanumeric, max 8 chars).')
@minLength(1)
@maxLength(8)
param projectName string = 'assetmgr'

@description('Administrator login username for the PostgreSQL server.')
param postgresAdminLogin string = 'pgadmin'

@description('Administrator login password for the PostgreSQL server. Must be at least 8 characters and contain uppercase, lowercase, digits, and non-alphanumeric characters.')
@secure()
@minLength(8)
param postgresAdminPassword string

@description('Name of the PostgreSQL database.')
param postgresDatabaseName string = 'assets_manager'

@description('Name of the Service Bus queue for image processing messages.')
param serviceBusQueueName string = 'image-processing'

@description('Name of the blob container for original uploaded assets.')
param assetsContainerName string = 'assets'

@description('Name of the blob container for generated thumbnails.')
param thumbnailsContainerName string = 'thumbnails'

// Unique suffix derived from the resource group ID - ensures globally unique names
var uniqueSuffix = take(uniqueString(resourceGroup().id), 8)
var resourcePrefix = '${projectName}-${environmentName}'

// Storage account name: lowercase alphanumeric, 3-24 chars
var storageAccountName = take(toLower('st${projectName}${environmentName}${uniqueSuffix}'), 24)

// Service Bus namespace: 6-50 chars, alphanumeric and hyphens
var serviceBusNamespaceName = 'sb-${resourcePrefix}-${uniqueSuffix}'

// PostgreSQL server name: 3-63 chars, lowercase letters, numbers, hyphens
var postgresServerName = 'psql-${resourcePrefix}-${uniqueSuffix}'

// Managed identity name
var managedIdentityName = 'id-${resourcePrefix}'

// ── Modules ──────────────────────────────────────────────────────────────────

module identity 'modules/identity.bicep' = {
  name: 'identity'
  params: {
    location: location
    identityName: managedIdentityName
  }
}

module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    location: location
    storageAccountName: storageAccountName
    assetsContainerName: assetsContainerName
    thumbnailsContainerName: thumbnailsContainerName
    managedIdentityPrincipalId: identity.outputs.principalId
  }
}

module serviceBus 'modules/servicebus.bicep' = {
  name: 'servicebus'
  params: {
    location: location
    namespaceName: serviceBusNamespaceName
    queueName: serviceBusQueueName
    managedIdentityPrincipalId: identity.outputs.principalId
  }
}

module postgresql 'modules/postgresql.bicep' = {
  name: 'postgresql'
  params: {
    location: location
    serverName: postgresServerName
    administratorLogin: postgresAdminLogin
    administratorLoginPassword: postgresAdminPassword
    databaseName: postgresDatabaseName
    managedIdentityPrincipalId: identity.outputs.principalId
    managedIdentityName: identity.outputs.name
  }
}

// ── Outputs ───────────────────────────────────────────────────────────────────

@description('Resource ID of the user-assigned managed identity')
output managedIdentityId string = identity.outputs.id

@description('Client ID of the managed identity - configure as AZURE_CLIENT_ID in applications')
output managedIdentityClientId string = identity.outputs.clientId

@description('Name of the storage account')
output storageAccountName string = storage.outputs.storageAccountName

@description('Blob service endpoint URL - configure as AZURE_STORAGE_ACCOUNT_URL in applications')
output storageAccountEndpoint string = storage.outputs.blobEndpoint

@description('Name of the assets blob container')
output assetsContainerName string = assetsContainerName

@description('Name of the thumbnails blob container')
output thumbnailsContainerName string = thumbnailsContainerName

@description('Fully qualified domain name of the Service Bus namespace')
output serviceBusHostname string = serviceBus.outputs.fullyQualifiedNamespace

@description('Name of the Service Bus queue for image processing')
output serviceBusQueueName string = serviceBus.outputs.queueName

@description('Fully qualified domain name of the PostgreSQL server')
output postgresFqdn string = postgresql.outputs.fullyQualifiedDomainName

@description('Name of the PostgreSQL server')
output postgresServerName string = postgresql.outputs.serverName

@description('Name of the PostgreSQL database')
output postgresDatabaseName string = postgresql.outputs.databaseName
