// User-Assigned Managed Identity module
// Provides a single identity used by web and worker services to authenticate
// with Azure Blob Storage, Service Bus, and PostgreSQL without credentials.

@description('Azure region for the managed identity resource.')
param location string

@description('Name of the user-assigned managed identity resource.')
param identityName string

resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
}

@description('Resource ID of the managed identity.')
output id string = managedIdentity.id

@description('Principal ID of the managed identity (used for RBAC role assignments).')
output principalId string = managedIdentity.properties.principalId

@description('Client ID of the managed identity (used in application configuration).')
output clientId string = managedIdentity.properties.clientId
