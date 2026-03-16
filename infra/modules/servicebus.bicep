// Azure Service Bus module
// Provisions a Service Bus namespace with an image-processing queue that
// replaces the RabbitMQ broker used by the web and worker modules.
// Queue settings mirror the existing RabbitMQ retry policy (3 attempts, 1 min delay).
// Grants the managed identity Azure Service Bus Data Owner access.

@description('Azure region for the Service Bus namespace.')
param location string

@description('Name of the Service Bus namespace.')
param namespaceName string

@description('Service Bus namespace pricing tier.')
@allowed(['Basic', 'Standard', 'Premium'])
param serviceBusSku string

@description('Principal ID of the managed identity to assign Service Bus Data Owner role.')
param managedIdentityPrincipalId string

// Queue name matching the existing RabbitMQ queue used by the application
var imageProcessingQueueName = 'image-processing'

// Built-in Azure Service Bus Data Owner role definition ID
var serviceBusDataOwnerRoleId = subscriptionResourceId(
  'Microsoft.Authorization/roleDefinitions',
  '090c5cfd-751d-490a-894a-3ce6f1109419'
)

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2021-11-01' = {
  name: namespaceName
  location: location
  sku: {
    name: serviceBusSku
    tier: serviceBusSku
  }
  properties: {}
}

// image-processing queue: mirrors worker retry policy (3 attempts, 60-second lock)
resource imageProcessingQueue 'Microsoft.ServiceBus/namespaces/queues@2021-11-01' = {
  parent: serviceBusNamespace
  name: imageProcessingQueueName
  properties: {
    // Lock duration matches the worker retry delay (PT1M = 1 minute)
    lockDuration: 'PT1M'
    // maxDeliveryCount matches the worker max attempts (3)
    maxDeliveryCount: 3
    deadLetteringOnMessageExpiration: true
    requiresDuplicateDetection: false
    requiresSession: false
    defaultMessageTimeToLive: 'P1D'
    enableBatchedOperations: true
  }
}

// Grant the managed identity full send/receive/manage permissions on this namespace
resource serviceBusDataOwnerAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, managedIdentityPrincipalId, serviceBusDataOwnerRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: serviceBusDataOwnerRoleId
    principalId: managedIdentityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Name of the provisioned Service Bus namespace.')
output namespaceName string = serviceBusNamespace.name

@description('Fully qualified Service Bus namespace hostname for AMQP connections.')
output namespaceHostname string = '${serviceBusNamespace.name}.servicebus.windows.net'

@description('Name of the image-processing queue.')
output queueName string = imageProcessingQueueName
