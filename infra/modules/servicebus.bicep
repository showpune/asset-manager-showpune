// Azure Service Bus module
// Provisions a Service Bus namespace with queues used for thumbnail generation
// messaging between the web and worker modules.
// Grants the provided managed identity the 'Azure Service Bus Data Owner' role.

@description('Azure region for deployment')
param location string

@description('Name of the Service Bus namespace (globally unique)')
param namespaceName string

@description('Name of the queue used for thumbnail generation messages')
param thumbnailQueueName string = 'thumbnail-requests'

@description('Principal ID of the managed identity to grant Service Bus Data Owner access')
param managedIdentityPrincipalId string

@description('Tags to apply to resources')
param tags object = {}

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' = {
  name: namespaceName
  location: location
  tags: tags
  sku: {
    name: 'Standard'
    tier: 'Standard'
  }
  properties: {
    minimumTlsVersion: '1.2'
    disableLocalAuth: false
  }
}

resource thumbnailQueue 'Microsoft.ServiceBus/namespaces/queues@2022-10-01-preview' = {
  parent: serviceBusNamespace
  name: thumbnailQueueName
  properties: {
    lockDuration: 'PT1M'
    maxSizeInMegabytes: 1024
    requiresDuplicateDetection: false
    requiresSession: false
    defaultMessageTimeToLive: 'P14D'
    deadLetteringOnMessageExpiration: true
    enableBatchedOperations: true
    maxDeliveryCount: 10
  }
}

// Azure Service Bus Data Owner – allows the managed identity to send and receive messages
var serviceBusDataOwnerRoleId = '090c5cfd-751d-490a-894a-3ce6f1109419'

resource serviceBusDataOwnerAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, managedIdentityPrincipalId, serviceBusDataOwnerRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', serviceBusDataOwnerRoleId)
    principalId: managedIdentityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Resource ID of the Service Bus namespace')
output namespaceid string = serviceBusNamespace.id

@description('Name of the Service Bus namespace')
output namespaceName string = serviceBusNamespace.name

@description('Fully-qualified Service Bus hostname (used as the connection endpoint)')
output namespaceHostname string = '${serviceBusNamespace.name}.servicebus.windows.net'

@description('Name of the thumbnail requests queue')
output thumbnailQueueName string = thumbnailQueue.name
