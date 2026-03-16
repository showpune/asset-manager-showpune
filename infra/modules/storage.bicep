// Azure Blob Storage module for asset storage
@description('Location for the storage account')
param location string

@description('Name of the storage account')
param storageAccountName string

@description('SKU for the storage account')
@allowed(['Standard_LRS', 'Standard_GRS', 'Standard_ZRS', 'Standard_RAGRS'])
param storageSku string = 'Standard_LRS'

@description('Name of the blob container for assets')
param containerName string = 'assets'

@description('Tags to apply to resources')
param tags object = {}

@description('Principal ID of the managed identity to grant Storage Blob Data Contributor role')
param webAppPrincipalId string

@description('Principal ID of the worker managed identity to grant Storage Blob Data Contributor role')
param workerAppPrincipalId string

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  tags: tags
  sku: {
    name: storageSku
  }
  kind: 'StorageV2'
  properties: {
    accessTier: 'Hot'
    allowBlobPublicAccess: false
    allowSharedKeyAccess: false
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    networkAcls: {
      defaultAction: 'Allow'
      bypass: 'AzureServices'
    }
    encryption: {
      services: {
        blob: {
          enabled: true
        }
        file: {
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
  name: containerName
  properties: {
    publicAccess: 'None'
  }
}

// Storage Blob Data Contributor role for web app managed identity
var storageBlobDataContributorRoleId = 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'

resource webAppStorageRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, webAppPrincipalId, storageBlobDataContributorRoleId)
  scope: storageAccount
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', storageBlobDataContributorRoleId)
    principalId: webAppPrincipalId
    principalType: 'ServicePrincipal'
  }
}

resource workerStorageRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, workerAppPrincipalId, storageBlobDataContributorRoleId)
  scope: storageAccount
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', storageBlobDataContributorRoleId)
    principalId: workerAppPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Storage account name')
output storageAccountName string = storageAccount.name

@description('Storage account blob endpoint')
output blobEndpoint string = storageAccount.properties.primaryEndpoints.blob

@description('Assets container name')
output containerName string = assetsContainer.name

@description('Storage account resource ID')
output storageAccountId string = storageAccount.id
