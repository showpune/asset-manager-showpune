/*
  Main Bicep template for assets-manager infrastructure.

  Resources provisioned:
    - User-Assigned Managed Identity (shared by web and worker)
    - Azure Blob Storage Account + container for asset files
    - Azure Service Bus Namespace + image-processing queue
    - Azure Database for PostgreSQL Flexible Server + assets_manager database
*/

@description('Azure region for all resources. Defaults to the resource group location.')
param location string = resourceGroup().location

@description('Application name prefix used for all resource names.')
param appName string = 'assets-manager'

@description('Deployment environment.')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'dev'

@description('Storage account SKU.')
@allowed(['Standard_LRS', 'Standard_GRS', 'Standard_ZRS', 'Standard_RAGRS', 'Premium_LRS'])
param storageSkuName string = 'Standard_LRS'

@description('Azure Service Bus namespace pricing tier. Standard or Premium required for queues.')
@allowed(['Standard', 'Premium'])
param serviceBusTier string = 'Standard'

@description('PostgreSQL server compute SKU name.')
param postgresSkuName string = 'Standard_D2ds_v4'

@description('PostgreSQL server compute tier.')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param postgresSkuTier string = 'GeneralPurpose'

@description('PostgreSQL server storage size in GB.')
param postgresStorageSizeGB int = 32

@description('PostgreSQL major version.')
@allowed(['14', '15', '16'])
param postgresVersion string = '16'

@description('PostgreSQL administrator login name.')
param postgresAdminLogin string = 'pgadmin'

@secure()
@description('PostgreSQL administrator password. Must be at least 8 characters and include uppercase, lowercase, digit, and special character.')
param postgresAdminPassword string

@description('Backup retention days for PostgreSQL.')
@minValue(7)
@maxValue(35)
param postgresBackupRetentionDays int = 7

var tags = {
  application: appName
  environment: environment
  managedBy: 'bicep'
}

// ── Managed Identity ────────────────────────────────────────────────────────

module identity 'modules/managedidentity.bicep' = {
  name: 'identity'
  params: {
    location: location
    appName: appName
    environment: environment
    tags: tags
  }
}

// ── Azure Blob Storage ───────────────────────────────────────────────────────

module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    location: location
    appName: appName
    environment: environment
    managedIdentityPrincipalId: identity.outputs.principalId
    skuName: storageSkuName
    tags: tags
  }
}

// ── Azure Service Bus ────────────────────────────────────────────────────────

module servicebus 'modules/servicebus.bicep' = {
  name: 'servicebus'
  params: {
    location: location
    appName: appName
    environment: environment
    managedIdentityPrincipalId: identity.outputs.principalId
    skuTier: serviceBusTier
    tags: tags
  }
}

// ── Azure Database for PostgreSQL Flexible Server ────────────────────────────

module postgresql 'modules/postgresql.bicep' = {
  name: 'postgresql'
  params: {
    location: location
    appName: appName
    environment: environment
    adminLogin: postgresAdminLogin
    adminPassword: postgresAdminPassword
    managedIdentityId: identity.outputs.id
    managedIdentityClientId: identity.outputs.clientId
    managedIdentityPrincipalId: identity.outputs.principalId
    managedIdentityName: identity.outputs.name
    skuName: postgresSkuName
    skuTier: postgresSkuTier
    storageSizeGB: postgresStorageSizeGB
    postgresVersion: postgresVersion
    backupRetentionDays: postgresBackupRetentionDays
    tags: tags
  }
}

// ── Outputs ──────────────────────────────────────────────────────────────────

@description('Client ID of the user-assigned managed identity. Set as AZURE_CLIENT_ID environment variable in both web and worker services.')
output managedIdentityClientId string = identity.outputs.clientId

@description('Resource ID of the user-assigned managed identity. Reference in App Service / Container Apps configuration.')
output managedIdentityId string = identity.outputs.id

@description('Name of the provisioned storage account.')
output storageAccountName string = storage.outputs.storageAccountName

@description('Name of the blob container for asset files. Replaces AWS S3 bucket name.')
output storageContainerName string = storage.outputs.containerName

@description('Primary blob service endpoint URL.')
output storageBlobEndpoint string = storage.outputs.blobEndpoint

@description('Name of the Service Bus namespace.')
output serviceBusNamespace string = servicebus.outputs.namespaceName

@description('Service Bus namespace hostname used as the connection endpoint. Replaces RabbitMQ host.')
output serviceBusEndpoint string = servicebus.outputs.endpoint

@description('Name of the image-processing queue. Matches the existing RabbitMQ queue name.')
output serviceBusQueueName string = servicebus.outputs.queueName

@description('Fully qualified hostname of the PostgreSQL server.')
output postgresHostname string = postgresql.outputs.hostname

@description('Port of the PostgreSQL server.')
output postgresPort int = postgresql.outputs.port

@description('Name of the application database.')
output postgresDatabaseName string = postgresql.outputs.databaseName

@description('JDBC connection string (password-based) for migration phase.')
output postgresJdbcUrl string = postgresql.outputs.jdbcConnectionString

@description('JDBC connection string using Managed Identity (passwordless, recommended).')
output postgresJdbcUrlMI string = postgresql.outputs.jdbcConnectionStringMI
