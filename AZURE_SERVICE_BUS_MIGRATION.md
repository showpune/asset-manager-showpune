# Azure Service Bus Migration

This document describes the migration from RabbitMQ to Azure Service Bus that was completed for the asset manager application.

## Overview

The application was successfully migrated from RabbitMQ messaging to Azure Service Bus while preserving all existing functionality including:
- Message sending from web module to worker module
- Manual message acknowledgment
- Retry logic for failed message processing
- Queue-based messaging pattern

## Architecture Changes

### Before (RabbitMQ)
- **Dependencies**: `spring-boot-starter-amqp`
- **Configuration**: `RabbitConfig` with `Queue`, `MessageConverter`, and `RabbitListenerContainerFactory`
- **Message Sending**: `RabbitTemplate.convertAndSend(queueName, message)`
- **Message Receiving**: `@RabbitListener(queues = "image-processing")`
- **Acknowledgment**: `Channel.basicAck()` / `Channel.basicNack()`

### After (Azure Service Bus)
- **Dependencies**: `spring-cloud-azure-starter`, `spring-messaging-azure-servicebus`
- **Configuration**: `ServiceBusConfig` with conditional loading and queue creation
- **Message Sending**: `ServiceBusTemplate.send(queueName, MessageBuilder.withPayload(message).build())`
- **Message Receiving**: `@ServiceBusListener(destination = "image-processing")`
- **Acknowledgment**: `ServiceBusReceivedMessageContext.complete()` / `context.abandon()`

## Key Migration Points

### Dependencies Updated
```xml
<!-- Removed -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Added -->
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-messaging-azure-servicebus</artifactId>
</dependency>
```

### Configuration Changes
- Created `ServiceBusConfig` with Azure Managed Identity support
- Added conditional loading to prevent issues during testing
- Configured automatic queue creation using `ServiceBusAdministrationClient`
- Maintained retry logic configuration for worker module

### Application Properties Updated
```properties
# Old RabbitMQ Configuration (commented out)
# spring.rabbitmq.host=localhost
# spring.rabbitmq.port=5672
# spring.rabbitmq.username=guest
# spring.rabbitmq.password=guest

# New Azure Service Bus Configuration
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}
```

### Code Changes

#### Message Producers (Web Module)
```java
// Before
rabbitTemplate.convertAndSend(QUEUE_NAME, message);

// After
Message<ImageProcessingMessage> serviceBusMessage = MessageBuilder.withPayload(message).build();
serviceBusTemplate.send(QUEUE_NAME, serviceBusMessage);
```

#### Message Consumers (Worker Module)
```java
// Before
@RabbitListener(queues = QUEUE_NAME)
public void processImage(final ImageProcessingMessage message, 
                       Channel channel, 
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    try {
        // Process message
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        channel.basicNack(deliveryTag, false, true);
    }
}

// After
@ServiceBusListener(destination = QUEUE_NAME)
public void processImage(ImageProcessingMessage payload, 
                       Message<ImageProcessingMessage> message,
                       @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context) {
    try {
        // Process message
        context.complete();
    } catch (Exception e) {
        context.abandon();
    }
}
```

## Testing Configuration

To support testing without requiring actual Azure Service Bus connections:
- Added test-specific application properties with `spring.cloud.azure.servicebus.enabled=false`
- Created `TestServiceBusConfig` with mock `ServiceBusTemplate` bean
- Updated test profiles to exclude Azure Service Bus auto-configuration

## Environment Variables Required

For production deployment, the following environment variables must be set:
- `AZURE_CLIENT_ID`: The client ID of the managed identity
- `SERVICE_BUS_NAMESPACE`: The Azure Service Bus namespace (e.g., `my-servicebus.servicebus.windows.net`)

## Benefits of Migration

1. **Cloud Native**: Better integration with Azure ecosystem
2. **Managed Identity**: Secure authentication without connection strings
3. **Scalability**: Azure Service Bus provides better scaling capabilities
4. **Reliability**: Built-in dead letter queues and message persistence
5. **Monitoring**: Better integration with Azure monitoring and logging

## Verification

The migration has been verified through:
- ✅ Successful compilation of both web and worker modules
- ✅ Passing unit tests with mock configuration
- ✅ Preservation of all existing message processing logic
- ✅ Maintained retry and error handling patterns
- ✅ Conditional configuration for different environments