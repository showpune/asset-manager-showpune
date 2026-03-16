@description('The Azure region to deploy Service Bus resources')
param location string

@description('The name of the Service Bus namespace (6-50 chars, alphanumeric and hyphens)')
param namespaceName string

@description('The name of the queue for image processing messages')
param queueName string

@description('Principal ID of the managed identity to grant Azure Service Bus Data Owner access')
param managedIdentityPrincipalId string

// Azure Service Bus Data Owner built-in role ID
var azureServiceBusDataOwnerRoleId = '090c5cfd-751d-490a-894a-3ce6f1109419'

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2021-11-01' = {
  name: namespaceName
  location: location
  sku: {
    name: 'Standard'
    tier: 'Standard'
  }
  properties: {
    disableLocalAuth: false
  }
}

resource imageProcessingQueue 'Microsoft.ServiceBus/namespaces/queues@2021-11-01' = {
  parent: serviceBusNamespace
  name: queueName
  properties: {
    // Lock duration matches worker retry delay (60 seconds)
    lockDuration: 'PT1M'
    maxSizeInMegabytes: 1024
    requiresDuplicateDetection: false
    requiresSession: false
    defaultMessageTimeToLive: 'P14D'
    deadLetteringOnMessageExpiration: true
    enableBatchedOperations: true
    // Matches worker retry configuration (3 attempts)
    maxDeliveryCount: 3
  }
}

resource serviceBusDataOwnerRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, managedIdentityPrincipalId, azureServiceBusDataOwnerRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', azureServiceBusDataOwnerRoleId)
    principalId: managedIdentityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Name of the Service Bus namespace')
output namespaceName string = serviceBusNamespace.name

@description('Fully qualified domain name of the Service Bus namespace')
output fullyQualifiedNamespace string = '${serviceBusNamespace.name}.servicebus.windows.net'

@description('Name of the image processing queue')
output queueName string = imageProcessingQueue.name

@description('Resource ID of the Service Bus namespace')
output serviceBusId string = serviceBusNamespace.id
