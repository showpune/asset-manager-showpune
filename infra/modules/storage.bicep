@description('Azure region for the resource')
param location string

@description('Application name prefix used for resource naming')
param appName string

@description('Deployment environment (dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environment string

@description('Principal ID of the managed identity to grant Storage Blob Data Contributor access')
param managedIdentityPrincipalId string

@description('Storage account SKU')
@allowed(['Standard_LRS', 'Standard_GRS', 'Standard_ZRS', 'Standard_RAGRS', 'Premium_LRS'])
param skuName string = 'Standard_LRS'

@description('Name of the blob container for asset files')
param containerName string = 'assets'

@description('Resource tags')
param tags object = {}

var cleanAppName = take(replace(replace(toLower(appName), '-', ''), '_', ''), 8)
var uniqueSuffix = take(uniqueString(resourceGroup().id), 6)
var storageAccountName = 'st${cleanAppName}${environment}${uniqueSuffix}'

// Built-in role: Storage Blob Data Contributor
var storageBlobDataContributorRoleId = 'ba92f5b4-2d11-453d-a403-e96b0029c9fe'

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  tags: tags
  sku: {
    name: skuName
  }
  kind: 'StorageV2'
  properties: {
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    allowBlobPublicAccess: false
    allowSharedKeyAccess: true
    defaultToOAuthAuthentication: false
    accessTier: 'Hot'
    encryption: {
      services: {
        blob: {
          enabled: true
          keyType: 'Account'
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
    containerDeleteRetentionPolicy: {
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

// Grant Storage Blob Data Contributor to the managed identity
resource storageBlobDataContributorRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storageAccount.id, managedIdentityPrincipalId, storageBlobDataContributorRoleId)
  scope: storageAccount
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      storageBlobDataContributorRoleId
    )
    principalId: managedIdentityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Name of the storage account')
output storageAccountName string = storageAccount.name

@description('Name of the blob container for assets')
output containerName string = assetsContainer.name

@description('Primary blob endpoint URL')
output blobEndpoint string = storageAccount.properties.primaryEndpoints.blob

@description('Resource ID of the storage account')
output storageAccountId string = storageAccount.id
