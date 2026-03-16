// Main Bicep template for Asset Manager infrastructure
// Provisions: Storage Account, Service Bus, PostgreSQL Flexible Server, Managed Identities
targetScope = 'resourceGroup'

@description('Location for all resources. Defaults to resource group location.')
param location string = resourceGroup().location

@description('Environment name (dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environmentName string = 'dev'

@description('Application name used as base for resource naming')
param appName string = 'asset-manager'

@description('PostgreSQL administrator login username')
param postgresAdminLogin string = 'pgadmin'

@description('PostgreSQL administrator login password')
@secure()
param postgresAdminPassword string

@description('PostgreSQL SKU name')
param postgresSkuName string = 'Standard_D2s_v3'

@description('PostgreSQL SKU tier')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param postgresSkuTier string = 'GeneralPurpose'

@description('PostgreSQL storage size in GB')
param postgresStorageSizeGB int = 32

@description('PostgreSQL version')
@allowed(['14', '15', '16'])
param postgresVersion string = '16'

@description('Storage account SKU')
@allowed(['Standard_LRS', 'Standard_GRS', 'Standard_ZRS', 'Standard_RAGRS'])
param storageSku string = 'Standard_LRS'

@description('Service Bus SKU')
@allowed(['Basic', 'Standard', 'Premium'])
param serviceBusSku string = 'Standard'

@description('Tags to apply to all resources')
param tags object = {
  environment: environmentName
  application: appName
  managedBy: 'bicep'
}

// Unique suffix based on resource group ID to avoid naming collisions
var uniqueSuffix = uniqueString(resourceGroup().id)
var shortSuffix = substring(uniqueSuffix, 0, 6)

// Resource names
var webAppIdentityName = '${appName}-web-identity-${environmentName}'
var workerIdentityName = '${appName}-worker-identity-${environmentName}'
var storageAccountName = 'assets${shortSuffix}${environmentName}'
var serviceBusNamespaceName = '${appName}-sb-${environmentName}-${shortSuffix}'
var postgresServerName = '${appName}-pg-${environmentName}-${shortSuffix}'

// Deploy managed identities first (other modules depend on their principal IDs)
module managedIdentity 'modules/managed-identity.bicep' = {
  name: 'managedIdentity'
  params: {
    location: location
    webAppIdentityName: webAppIdentityName
    workerIdentityName: workerIdentityName
    tags: tags
  }
}

// Deploy Storage Account
module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    location: location
    storageAccountName: storageAccountName
    storageSku: storageSku
    containerName: 'assets'
    tags: tags
    webAppPrincipalId: managedIdentity.outputs.webAppPrincipalId
    workerAppPrincipalId: managedIdentity.outputs.workerPrincipalId
  }
}

// Deploy Service Bus Namespace and queues
module serviceBus 'modules/servicebus.bicep' = {
  name: 'serviceBus'
  params: {
    location: location
    serviceBusNamespaceName: serviceBusNamespaceName
    serviceBusSku: serviceBusSku
    tags: tags
    webAppPrincipalId: managedIdentity.outputs.webAppPrincipalId
    workerAppPrincipalId: managedIdentity.outputs.workerPrincipalId
  }
}

// Deploy PostgreSQL Flexible Server
module postgresql 'modules/postgresql.bicep' = {
  name: 'postgresql'
  params: {
    location: location
    serverName: postgresServerName
    administratorLogin: postgresAdminLogin
    administratorPassword: postgresAdminPassword
    skuName: postgresSkuName
    skuTier: postgresSkuTier
    postgresVersion: postgresVersion
    storageSizeGB: postgresStorageSizeGB
    databaseName: 'assets_manager'
    tags: tags
    webAppObjectId: managedIdentity.outputs.webAppObjectId
    webAppIdentityName: managedIdentity.outputs.webAppIdentityName
  }
}

// ─── Outputs ────────────────────────────────────────────────────────────────

@description('Web app managed identity resource ID')
output webAppIdentityId string = managedIdentity.outputs.webAppIdentityId

@description('Web app managed identity client ID (use as AZURE_CLIENT_ID)')
output webAppClientId string = managedIdentity.outputs.webAppClientId

@description('Worker managed identity resource ID')
output workerIdentityId string = managedIdentity.outputs.workerIdentityId

@description('Worker managed identity client ID (use as AZURE_CLIENT_ID)')
output workerClientId string = managedIdentity.outputs.workerClientId

@description('Storage account name')
output storageAccountName string = storage.outputs.storageAccountName

@description('Storage account blob endpoint')
output blobEndpoint string = storage.outputs.blobEndpoint

@description('Assets container name')
output containerName string = storage.outputs.containerName

@description('Service Bus namespace name')
output serviceBusNamespaceName string = serviceBus.outputs.serviceBusNamespaceName

@description('Service Bus fully qualified domain name endpoint')
output serviceBusEndpoint string = serviceBus.outputs.serviceBusEndpoint

@description('Image processing queue name')
output imageProcessingQueueName string = serviceBus.outputs.imageProcessingQueueName

@description('PostgreSQL server FQDN')
output postgresServerFqdn string = postgresql.outputs.serverFqdn

@description('PostgreSQL database name')
output postgresDatabaseName string = postgresql.outputs.databaseName

@description('PostgreSQL JDBC connection string (passwordless, use with Managed Identity)')
output postgresJdbcUrl string = postgresql.outputs.jdbcConnectionString
