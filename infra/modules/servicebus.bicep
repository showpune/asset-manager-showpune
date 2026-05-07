@description('Location for the Service Bus namespace.')
param location string

@description('Environment name for resource token generation.')
param environmentName string

@description('Principal ID of the managed identity for role assignment.')
param identityPrincipalId string

var resourceToken = uniqueString(subscription().id, resourceGroup().id, location, environmentName)
var serviceBusNamespaceName = 'azsb${resourceToken}'
var queueName = 'image-processing'

// Azure Service Bus Data Owner
var serviceBusDataOwnerRoleId = '090c5cfd-751d-490a-894a-3ce6f1109419'

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2023-01-01-preview' = {
  name: serviceBusNamespaceName
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
  }
}

resource serviceBusQueue 'Microsoft.ServiceBus/namespaces/queues@2023-01-01-preview' = {
  parent: serviceBusNamespace
  name: queueName
  properties: {
    maxSizeInMegabytes: 1024
    defaultMessageTimeToLive: 'P14D'
  }
}

resource serviceBusDataOwnerRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, identityPrincipalId, serviceBusDataOwnerRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', serviceBusDataOwnerRoleId)
    principalId: identityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

output serviceBusNamespaceName string = serviceBusNamespace.name
output serviceBusNamespaceId string = serviceBusNamespace.id
output serviceBusQueueName string = serviceBusQueue.name
output serviceBusEndpoint string = '${serviceBusNamespace.name}.servicebus.windows.net'
