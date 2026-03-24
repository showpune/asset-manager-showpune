// Azure Service Bus module for message processing between web and worker modules
@description('Location for the Service Bus namespace')
param location string

@description('Name of the Service Bus namespace')
param serviceBusNamespaceName string

@description('SKU for the Service Bus namespace')
@allowed(['Basic', 'Standard', 'Premium'])
param serviceBusSku string = 'Standard'

@description('Tags to apply to resources')
param tags object = {}

@description('Principal ID of the web app managed identity')
param webAppPrincipalId string

@description('Principal ID of the worker managed identity')
param workerAppPrincipalId string

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2021-11-01' = {
  name: serviceBusNamespaceName
  location: location
  tags: tags
  sku: {
    name: serviceBusSku
    tier: serviceBusSku
  }
  properties: {
    disableLocalAuth: true
  }
}

// Image processing queue - maps to RabbitMQ "image-processing" queue
resource imageProcessingQueue 'Microsoft.ServiceBus/namespaces/queues@2021-11-01' = {
  parent: serviceBusNamespace
  name: 'image-processing'
  properties: {
    lockDuration: 'PT5M'
    maxSizeInMegabytes: 1024
    requiresDuplicateDetection: false
    requiresSession: false
    defaultMessageTimeToLive: 'P14D'
    deadLetteringOnMessageExpiration: true
    enableBatchedOperations: true
    maxDeliveryCount: 10
    enablePartitioning: false
  }
}

// Dead letter queue is automatically created by Azure Service Bus

// Azure Service Bus Data Sender role for web app (sends messages)
var serviceBusDataSenderRoleId = '69a216fc-b8fb-44d8-bc22-1f3c2cd27a39'

resource webAppServiceBusSenderRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, webAppPrincipalId, serviceBusDataSenderRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', serviceBusDataSenderRoleId)
    principalId: webAppPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// Azure Service Bus Data Receiver role for worker (receives messages)
var serviceBusDataReceiverRoleId = '4f6d3b9b-027b-4f4c-9142-0e5a2a2247e0'

resource workerServiceBusReceiverRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, workerAppPrincipalId, serviceBusDataReceiverRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', serviceBusDataReceiverRoleId)
    principalId: workerAppPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// Also grant web app receiver role so it can also listen (backup processor profile)
resource webAppServiceBusReceiverRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, webAppPrincipalId, serviceBusDataReceiverRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', serviceBusDataReceiverRoleId)
    principalId: webAppPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Service Bus namespace name')
output serviceBusNamespaceName string = serviceBusNamespace.name

@description('Service Bus namespace fully qualified domain name')
output serviceBusEndpoint string = serviceBusNamespace.properties.serviceBusEndpoint

@description('Image processing queue name')
output imageProcessingQueueName string = imageProcessingQueue.name

@description('Service Bus namespace resource ID')
output serviceBusNamespaceId string = serviceBusNamespace.id
