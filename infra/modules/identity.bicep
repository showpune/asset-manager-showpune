// User-Assigned Managed Identity module
// Provides a single managed identity that can be assigned to all Azure resources

@description('Azure region for deployment')
param location string

@description('Name of the user-assigned managed identity')
param identityName string

@description('Tags to apply to resources')
param tags object = {}

resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
  tags: tags
}

@description('Resource ID of the managed identity')
output identityId string = managedIdentity.id

@description('Principal ID of the managed identity (used for RBAC assignments)')
output principalId string = managedIdentity.properties.principalId

@description('Client ID of the managed identity (used in application config)')
output clientId string = managedIdentity.properties.clientId
