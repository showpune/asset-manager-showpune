// Managed Identity module - creates user-assigned managed identities for web and worker apps
@description('Location for the managed identities')
param location string

@description('Name of the web app managed identity')
param webAppIdentityName string

@description('Name of the worker managed identity')
param workerIdentityName string

@description('Tags to apply to resources')
param tags object = {}

resource webAppIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: webAppIdentityName
  location: location
  tags: tags
}

resource workerIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: workerIdentityName
  location: location
  tags: tags
}

@description('Web app managed identity resource ID')
output webAppIdentityId string = webAppIdentity.id

@description('Web app managed identity principal ID')
output webAppPrincipalId string = webAppIdentity.properties.principalId

@description('Web app managed identity client ID')
output webAppClientId string = webAppIdentity.properties.clientId

@description('Web app managed identity object ID (same as principal ID)')
output webAppObjectId string = webAppIdentity.properties.principalId

@description('Web app managed identity name')
output webAppIdentityName string = webAppIdentity.name

@description('Worker managed identity resource ID')
output workerIdentityId string = workerIdentity.id

@description('Worker managed identity principal ID')
output workerPrincipalId string = workerIdentity.properties.principalId

@description('Worker managed identity client ID')
output workerClientId string = workerIdentity.properties.clientId

@description('Worker managed identity name')
output workerIdentityName string = workerIdentity.name
