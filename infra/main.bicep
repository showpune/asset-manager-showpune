// Asset Manager – Azure Infrastructure
// Main Bicep template that orchestrates all infrastructure modules:
//   - User-Assigned Managed Identity (credential-free auth for all services)
//   - Azure Blob Storage (replaces AWS S3 for asset/thumbnail storage)
//   - Azure Service Bus (replaces RabbitMQ for image-processing messages)
//   - Azure Database for PostgreSQL Flexible Server (managed PostgreSQL)

targetScope = 'resourceGroup'

// ── Parameters ─────────────────────────────────────────────────────────────

@description('Azure region for all resources. Defaults to the resource group location.')
param location string = resourceGroup().location

@description('Short environment identifier appended to resource names (dev, test, prod).')
@allowed(['dev', 'test', 'prod'])
param environmentName string = 'dev'

@description('Short project prefix used in resource names (max 10 alphanumeric characters, hyphens allowed).')
@maxLength(10)
param projectName string = 'assetmgr'

@description('PostgreSQL flexible server administrator username.')
@minLength(1)
param postgresAdminLogin string = 'pgadmin'

@description('PostgreSQL flexible server administrator password. Must meet Azure complexity requirements.')
@secure()
param postgresAdminPassword string

@description('Azure Blob Storage account replication SKU.')
@allowed(['Standard_LRS', 'Standard_GRS', 'Standard_ZRS', 'Premium_LRS'])
param storageSku string = 'Standard_LRS'

@description('Azure Service Bus namespace pricing tier. Standard or Premium required for queues.')
@allowed(['Standard', 'Premium'])
param serviceBusSku string = 'Standard'

@description('PostgreSQL flexible server compute SKU name.')
param postgresSkuName string = 'Standard_B2s'

@description('PostgreSQL flexible server compute tier.')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param postgresSkuTier string = 'Burstable'

@description('PostgreSQL flexible server storage size in GB.')
param postgresStorageSizeGB int = 32

// ── Variables ───────────────────────────────────────────────────────────────

var resourceSuffix = '${projectName}-${environmentName}'

// Storage account names must be 3-24 lowercase alphanumeric characters and globally unique
var storageAccountName = toLower('${replace(projectName, '-', '')}${uniqueString(resourceGroup().id)}')

// ── Modules ─────────────────────────────────────────────────────────────────

module identity 'modules/identity.bicep' = {
  name: 'identity-deployment'
  params: {
    location: location
    identityName: '${resourceSuffix}-id'
  }
}

module storage 'modules/storage.bicep' = {
  name: 'storage-deployment'
  params: {
    location: location
    storageAccountName: storageAccountName
    storageSku: storageSku
    managedIdentityPrincipalId: identity.outputs.principalId
  }
}

module serviceBus 'modules/servicebus.bicep' = {
  name: 'servicebus-deployment'
  params: {
    location: location
    namespaceName: '${resourceSuffix}-sb'
    serviceBusSku: serviceBusSku
    managedIdentityPrincipalId: identity.outputs.principalId
  }
}

module postgresql 'modules/postgresql.bicep' = {
  name: 'postgresql-deployment'
  params: {
    location: location
    serverName: '${resourceSuffix}-pg'
    administratorLogin: postgresAdminLogin
    administratorPassword: postgresAdminPassword
    skuName: postgresSkuName
    skuTier: postgresSkuTier
    storageSizeGB: postgresStorageSizeGB
    managedIdentityId: identity.outputs.id
  }
}

// ── Outputs ──────────────────────────────────────────────────────────────────

@description('Resource ID of the user-assigned managed identity.')
output managedIdentityId string = identity.outputs.id

@description('Client ID of the managed identity – set as AZURE_CLIENT_ID in application configuration.')
output managedIdentityClientId string = identity.outputs.clientId

@description('Name of the provisioned storage account.')
output storageAccountName string = storage.outputs.storageAccountName

@description('Blob service endpoint URL.')
output storageBlobEndpoint string = storage.outputs.blobEndpoint

@description('Name of the blob container for asset storage.')
output storageContainerName string = storage.outputs.containerName

@description('Name of the Service Bus namespace.')
output serviceBusNamespaceName string = serviceBus.outputs.namespaceName

@description('Service Bus namespace hostname (use with AMQP: amqps://<hostname>).')
output serviceBusHostname string = serviceBus.outputs.namespaceHostname

@description('Name of the image-processing queue.')
output serviceBusQueueName string = serviceBus.outputs.queueName

@description('Name of the PostgreSQL flexible server.')
output postgresServerName string = postgresql.outputs.serverName

@description('Fully qualified domain name of the PostgreSQL server.')
output postgresFqdn string = postgresql.outputs.fqdn

@description('Name of the application database.')
output postgresDatabaseName string = postgresql.outputs.databaseName
