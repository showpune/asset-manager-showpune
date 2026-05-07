@description('Environment name used for resource naming and unique token generation.')
param environmentName string = 'dev'

@description('Primary Azure region for all resources.')
param location string = 'eastus2'

@description('Azure region for the PostgreSQL flexible server (may differ if primary region is restricted).')
param postgresLocation string = 'westus2'

@description('Administrator login name for the PostgreSQL flexible server.')
param administratorLogin string = 'pgadmin'

@description('Administrator login password for the PostgreSQL flexible server.')
@secure()
param administratorLoginPassword string

// Managed Identity
module identity 'modules/identity.bicep' = {
  name: 'identity'
  params: {
    location: location
    environmentName: environmentName
  }
}

// Azure Storage Account + Blob container
module storage 'modules/storage.bicep' = {
  name: 'storage'
  params: {
    location: location
    environmentName: environmentName
    identityPrincipalId: identity.outputs.identityPrincipalId
  }
}

// Azure Service Bus Namespace + Queue
module servicebus 'modules/servicebus.bicep' = {
  name: 'servicebus'
  params: {
    location: location
    environmentName: environmentName
    identityPrincipalId: identity.outputs.identityPrincipalId
  }
}

// Azure Database for PostgreSQL Flexible Server
module postgresql 'modules/postgresql.bicep' = {
  name: 'postgresql'
  params: {
    location: postgresLocation
    environmentName: environmentName
    administratorLogin: administratorLogin
    administratorLoginPassword: administratorLoginPassword
    identityPrincipalId: identity.outputs.identityPrincipalId
    identityName: identity.outputs.identityName
  }
}

output managedIdentityId string = identity.outputs.identityId
output managedIdentityName string = identity.outputs.identityName
output managedIdentityClientId string = identity.outputs.identityClientId
output managedIdentityPrincipalId string = identity.outputs.identityPrincipalId
output storageAccountName string = storage.outputs.storageAccountName
output blobContainerName string = storage.outputs.blobContainerName
output serviceBusNamespace string = servicebus.outputs.serviceBusNamespaceName
output serviceBusEndpoint string = servicebus.outputs.serviceBusEndpoint
output serviceBusQueueName string = servicebus.outputs.serviceBusQueueName
output postgresServerName string = postgresql.outputs.postgresServerName
output postgresFqdn string = postgresql.outputs.postgresFqdn
output postgresDatabaseName string = postgresql.outputs.postgresDatabaseName
