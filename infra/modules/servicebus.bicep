@description('Azure region for the resource')
param location string

@description('Application name prefix used for resource naming')
param appName string

@description('Deployment environment (dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environment string

@description('Principal ID of the managed identity to grant Service Bus access')
param managedIdentityPrincipalId string

@description('Service Bus namespace SKU tier')
@allowed(['Basic', 'Standard', 'Premium'])
param skuTier string = 'Standard'

@description('Name of the queue for image processing messages (matches RabbitMQ queue name)')
param imageProcessingQueueName string = 'image-processing'

@description('Maximum delivery count before dead-lettering (matches worker retry configuration)')
param maxDeliveryCount int = 3

@description('Resource tags')
param tags object = {}

var uniqueSuffix = take(uniqueString(resourceGroup().id), 6)
var namespaceName = 'sb-${appName}-${environment}-${uniqueSuffix}'

// Built-in role: Azure Service Bus Data Owner
var serviceBusDataOwnerRoleId = '090c5cfd-751d-490a-894a-3ce6f1109419'

resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' = {
  name: namespaceName
  location: location
  tags: tags
  sku: {
    name: skuTier
    tier: skuTier
  }
  properties: {
    minimumTlsVersion: '1.2'
    disableLocalAuth: false
  }
}

resource imageProcessingQueue 'Microsoft.ServiceBus/namespaces/queues@2022-10-01-preview' = {
  parent: serviceBusNamespace
  name: imageProcessingQueueName
  properties: {
    lockDuration: 'PT5M'
    maxSizeInMegabytes: 1024
    requiresDuplicateDetection: false
    requiresSession: false
    defaultMessageTimeToLive: 'P7D'
    deadLetteringOnMessageExpiration: true
    duplicateDetectionHistoryTimeWindow: 'PT10M'
    maxDeliveryCount: maxDeliveryCount
    enableBatchedOperations: true
    enablePartitioning: false
    enableExpress: false
  }
}

// Grant Service Bus Data Owner to the managed identity
// For production, split into Data Sender (web) and Data Receiver (worker) for least privilege
resource serviceBusDataOwnerRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(serviceBusNamespace.id, managedIdentityPrincipalId, serviceBusDataOwnerRoleId)
  scope: serviceBusNamespace
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      serviceBusDataOwnerRoleId
    )
    principalId: managedIdentityPrincipalId
    principalType: 'ServicePrincipal'
  }
}

@description('Name of the Service Bus namespace')
output namespaceName string = serviceBusNamespace.name

@description('Name of the image processing queue')
output queueName string = imageProcessingQueue.name

@description('Fully qualified Service Bus namespace hostname (used as connection endpoint)')
output endpoint string = '${serviceBusNamespace.name}.servicebus.windows.net'

@description('Resource ID of the Service Bus namespace')
output namespaceId string = serviceBusNamespace.id
