# RabbitMQ to Azure Service Bus Migration

This document describes the migration from RabbitMQ to Azure Service Bus completed for the asset manager application.

## Migration Summary

### What Changed
- **Dependencies**: Replaced `spring-boot-starter-amqp` with Azure Service Bus dependencies
- **Configuration**: Replaced RabbitMQ configuration with Azure Service Bus configuration
- **Message Templates**: Replaced `RabbitTemplate` with `ServiceBusTemplate`
- **Message Listeners**: Replaced `@RabbitListener` with `@ServiceBusListener`
- **Authentication**: Switched to Azure Managed Identity instead of username/password

### Dependencies Added
```xml
<!-- Parent POM -->
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-dependencies</artifactId>
    <version>5.22.0</version>
    <scope>import</scope>
    <type>pom</type>
</dependency>

<!-- Both modules -->
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-messaging-azure-servicebus</artifactId>
</dependency>

<!-- Worker module only -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

### Configuration Required

Set these environment variables:
- `AZURE_CLIENT_ID`: Client ID for managed identity
- `SERVICE_BUS_NAMESPACE`: Azure Service Bus namespace (e.g., `your-namespace.servicebus.windows.net`)

### Code Changes

#### Message Sending (Before/After)
```java
// Before (RabbitMQ)
rabbitTemplate.convertAndSend(QUEUE_NAME, message);

// After (Azure Service Bus)
Message<ImageProcessingMessage> serviceBusMessage = MessageBuilder.withPayload(message).build();
serviceBusTemplate.send(QUEUE_NAME, serviceBusMessage);
```

#### Message Listening (Before/After)
```java
// Before (RabbitMQ)
@RabbitListener(queues = QUEUE_NAME)
public void processMessage(ImageProcessingMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    // manual acknowledgment with channel.basicAck()
}

// After (Azure Service Bus)
@ServiceBusListener(destination = QUEUE_NAME)
public void processMessage(ImageProcessingMessage message) {
    // automatic acknowledgment, throw exception to retry
}
```

### Azure Resources Setup

1. Create an Azure Service Bus namespace
2. Set up Managed Identity for your application
3. Grant the Managed Identity "Azure Service Bus Data Owner" role on the namespace
4. The queue will be created automatically on first use

### Backward Compatibility

The migration preserves:
- Same queue name: "image-processing"
- Same message structure: `ImageProcessingMessage`
- Same retry behavior (configured in RetryTemplate)
- Same business logic

### Rollback Plan

To rollback to RabbitMQ:
1. Revert the dependency changes in pom.xml files
2. Restore the RabbitConfig classes from git history
3. Update the service classes to use RabbitTemplate and @RabbitListener
4. Update application properties to use RabbitMQ settings

## Testing

The application compiles successfully and maintains the same interfaces. For full testing:
1. Deploy to an environment with Azure Service Bus
2. Set the required environment variables
3. Test file upload and thumbnail generation functionality