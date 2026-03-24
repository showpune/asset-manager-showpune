@description('The Azure region to deploy the managed identity')
param location string

@description('The name of the user-assigned managed identity')
param identityName string

resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
}

@description('Resource ID of the managed identity')
output id string = managedIdentity.id

@description('Principal (object) ID of the managed identity - used for RBAC role assignments')
output principalId string = managedIdentity.properties.principalId

@description('Client ID of the managed identity - used for application authentication')
output clientId string = managedIdentity.properties.clientId

@description('Name of the managed identity')
output name string = managedIdentity.name
