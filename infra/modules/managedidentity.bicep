@description('Azure region for the resource')
param location string

@description('Application name prefix used for resource naming')
param appName string

@description('Deployment environment (dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environment string

@description('Resource tags')
param tags object = {}

var identityName = 'id-${appName}-${environment}'

resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
  tags: tags
}

@description('Resource ID of the user-assigned managed identity')
output id string = managedIdentity.id

@description('Principal (object) ID of the managed identity, used for RBAC role assignments')
output principalId string = managedIdentity.properties.principalId

@description('Client ID of the managed identity, used by applications for authentication')
output clientId string = managedIdentity.properties.clientId

@description('Name of the managed identity resource')
output name string = managedIdentity.name
