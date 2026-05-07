@description('Location for the managed identity.')
param location string

@description('Environment name for resource token generation.')
param environmentName string

var resourceToken = uniqueString(subscription().id, resourceGroup().id, location, environmentName)
var identityName = 'azmi${resourceToken}'

resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
}

output identityId string = managedIdentity.id
output identityName string = managedIdentity.name
output identityClientId string = managedIdentity.properties.clientId
output identityPrincipalId string = managedIdentity.properties.principalId
