# Azure Service Bus Migration

This document describes the migration from RabbitMQ to Azure Service Bus for the asset manager application.

## Migration Overview

The application has been successfully migrated from RabbitMQ to Azure Service Bus while maintaining the same functional behavior:

- **Message Producer**: Web module sends `ImageProcessingMessage` to the "image-processing" queue
- **Message Consumer**: Worker module processes messages from the same queue
- **Functionality**: Thumbnail generation for uploaded images remains unchanged

## Key Changes

### Dependencies
- **Parent POM**: Added Azure Service Bus BOM (spring-cloud-azure-dependencies v4.20.0)
- **Web Module**: Replaced `spring-boot-starter-amqp` with Azure Service Bus dependencies
- **Worker Module**: Replaced `spring-boot-starter-amqp` with Azure Service Bus dependencies + spring-retry

### Configuration
- **Queue Setup**: Uses Azure Service Bus Queue instead of RabbitMQ Queue
- **Authentication**: Uses Azure Managed Identity (no connection strings)
- **Administration**: ServiceBusAdministrationClient for queue management

### Code Changes
- **Message Sending**: `RabbitTemplate` → `ServiceBusTemplate`
- **Message Receiving**: `@RabbitListener` → `@ServiceBusListener`
- **Message Acknowledgment**: `channel.basicAck()/basicNack()` → `context.complete()/abandon()`
- **Annotations**: `@EnableRabbit` → `@EnableAzureMessaging`

## Configuration Properties

### Required Environment Variables
```properties
AZURE_CLIENT_ID=<your-managed-identity-client-id>
SERVICE_BUS_NAMESPACE=<your-service-bus-namespace>
```

### Application Properties
```properties
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}
```

## Architecture

```
Web Module (Producer)
├── AwsS3Service.java - Sends messages after file upload
├── LocalFileStorageService.java - Sends messages after local file storage
└── ServiceBusConfig.java - Queue configuration

Worker Module (Consumer)
├── AbstractFileProcessingService.java - Processes messages for thumbnail generation
├── ServiceBusConfig.java - Queue configuration with retry template
└── Retry mechanism maintained for failed processing

Backup Module (Optional)
└── BackupMessageProcessor.java - Monitors messages (active only with "backup" profile)
```

## Message Flow

1. User uploads image via web interface
2. File stored in S3 or local storage
3. `ImageProcessingMessage` sent to "image-processing" queue
4. Worker receives message and generates thumbnail
5. Message completed on success or abandoned for retry on failure

## Benefits

- **Managed Identity**: Secure authentication without connection strings
- **Azure Integration**: Native Azure cloud service
- **Reliability**: Built-in retry and dead letter queue support
- **Monitoring**: Azure Service Bus provides comprehensive metrics
- **Scalability**: Azure Service Bus handles high throughput

## Testing

The migration includes basic unit tests to verify:
- Configuration constants are correct
- Dependencies are properly injected
- Spring context loads successfully

## Rollback

If rollback is needed, revert to RabbitMQ by:
1. Restore `spring-boot-starter-amqp` dependencies
2. Restore RabbitConfig classes
3. Replace ServiceBusTemplate with RabbitTemplate
4. Replace @ServiceBusListener with @RabbitListener
5. Restore RabbitMQ connection properties