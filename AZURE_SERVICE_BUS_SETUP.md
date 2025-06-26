# Azure Service Bus Configuration Guide

This document describes how to configure Azure Service Bus for the Asset Manager application after migration from RabbitMQ.

## Overview

The application has been migrated from RabbitMQ to Azure Service Bus with the following key changes:
- Queue-based messaging pattern preserved
- Same message format (ImageProcessingMessage) maintained
- Azure Managed Identity authentication
- Automatic message handling and retry policies

## Prerequisites

1. Azure Service Bus namespace
2. Azure Managed Identity configured with Service Bus permissions
3. Environment variables set for authentication

## Required Environment Variables

Set the following environment variables:

```bash
# Azure Managed Identity Client ID
export AZURE_CLIENT_ID=your-managed-identity-client-id

# Azure Service Bus Namespace (without .servicebus.windows.net)
export SERVICE_BUS_NAMESPACE=your-servicebus-namespace
```

## Application Configuration

The following properties are automatically configured in `application.properties`:

```properties
# Azure Service Bus Configuration
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}
```

## Queue Configuration

The application automatically creates the queue `image-processing` if it doesn't exist. The queue configuration:
- **Queue Name**: `image-processing`
- **Entity Type**: Queue (not Topic/Subscription)
- **Message Format**: JSON (ImageProcessingMessage)

## Key Migration Points

### Message Sending
- **Before**: `rabbitTemplate.convertAndSend(QUEUE_NAME, message)`
- **After**: `serviceBusTemplate.send(QUEUE_NAME, MessageBuilder.withPayload(message).build())`

### Message Receiving
- **Before**: `@RabbitListener(queues = QUEUE_NAME)`
- **After**: `@ServiceBusListener(destination = QUEUE_NAME)`

### Retry Logic
- Maintained the same retry template configuration (3 attempts, 1-minute delay)
- Azure Service Bus provides additional built-in retry mechanisms

### Error Handling
- Service Bus automatically handles message redelivery on exceptions
- Manual acknowledgment is handled automatically by the framework

## Testing

For testing, the Service Bus configuration is disabled and mocked components are used:
- Set profile to `test` to disable real Service Bus connections
- Mock beans are provided for `ServiceBusTemplate` and related components

## Production Deployment

1. Ensure Azure Managed Identity is properly configured
2. Set environment variables `AZURE_CLIENT_ID` and `SERVICE_BUS_NAMESPACE`
3. Deploy the application - the queue will be created automatically
4. Monitor message processing through Azure portal or application logs

## Troubleshooting

### Common Issues

1. **"'fullyQualifiedNamespace' cannot be null"**
   - Check that `SERVICE_BUS_NAMESPACE` environment variable is set
   - Ensure the namespace name doesn't include `.servicebus.windows.net`

2. **Authentication failures**
   - Verify Azure Managed Identity has Service Bus permissions
   - Check `AZURE_CLIENT_ID` is correctly set

3. **Queue not found**
   - The application automatically creates the queue if it doesn't exist
   - Ensure the Service Bus namespace exists and is accessible

### Required Azure Permissions

The Managed Identity needs the following permissions on the Service Bus namespace:
- `Azure Service Bus Data Owner` or
- `Azure Service Bus Data Sender` (for web module)
- `Azure Service Bus Data Receiver` (for worker module)