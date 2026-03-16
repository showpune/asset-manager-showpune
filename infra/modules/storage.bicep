// Azure Blob Storage module
// Provisions a StorageV2 account with a blob container for asset uploads.
// Grants the managed identity Storage Blob Data Contributor access.

@description('Azure region for the storage account.')
param location string

@description('Globally unique storage account name (3-24 lowercase alphanumeric characters).')
@minLength(3)
@maxLength(24)
param storageAccountName string

@description('Storage account replication SKU.')
@allowed(['Standard_LRS', 'Standard_GRS', 'Standard_ZRS', 'Premium_LRS'])
param storageSku string

@description('Principal ID of the managed identity to assign Storage Blob Data Contributor role.')
param managedIdentityPrincipalId string

// Container that holds uploaded assets and generated thumbnails
var assetsContainerName = 'assets'

// Built-in Storage Blob Data Contributor role definition ID
var storageBlobDataContributorRoleId = subscriptionResourceId(
  'Microsoft.Authorization/roleDefinitions',
  'ba92f5b4-2d11-453d-a403-e96b0029c9fe'
)

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  sku: {
    name: storageSku
  }
  kind: 'StorageV2'
  properties: {
    accessTier: 'Hot'
    allowBlobPublicAccess: false
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    encryption: {
      services: {
        blob: {
          enabled: true
        }
      }
      keySource: 'Microsoft.Storage'
    }
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-01-01' = {
  parent: storageAccount
  name: 'default'
  properties: {
    deleteRetentionPolicy: {
      enabled: true
      days: 7
    }
  }
}

resource assetsContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-01-01' = {
  parent: blobService
  name: assetsContainerName
  properties: {
    publicAccess: 'None'
  }
}

// Grant the managed identity permission to read/write blobs in this storage account
resource storageBlobDataContributorAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, managedIdentityPrincipalId, storageBlobDataContributorRoleId)
  scope: storageAccount
  properties: {
    roleDefinitionId: storageBlobDataContributorRoleId
    principalId: managedIdentityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Name of the provisioned storage account.')
output storageAccountName string = storageAccount.name

@description('Blob service endpoint URL.')
output blobEndpoint string = storageAccount.properties.primaryEndpoints.blob

@description('Name of the blob container for asset storage.')
output containerName string = assetsContainerName
