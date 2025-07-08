# Azure Service Bus Migration Guide

This document outlines the migration from RabbitMQ to Azure Service Bus completed in this project.

## Changes Made

### 1. Dependencies Updated

**Parent POM (`pom.xml`):**
- Added `spring-cloud-azure.version` property (5.22.0)
- Added `spring-cloud-azure-dependencies` BOM in dependencyManagement

**Web Module (`web/pom.xml`):**
- Removed `spring-boot-starter-amqp`
- Added `spring-cloud-azure-starter`
- Added `spring-messaging-azure-servicebus` 
- Added `spring-retry`

**Worker Module (`worker/pom.xml`):**
- Removed `spring-boot-starter-amqp`
- Added `spring-cloud-azure-starter`
- Added `spring-messaging-azure-servicebus`
- Added `spring-retry`

### 2. Configuration Changes

**RabbitConfig → ServiceBusConfig:**
- Replaced RabbitMQ queue configuration with Azure Service Bus queue configuration
- Added `@EnableAzureMessaging` annotation
- Added `ServiceBusAdministrationClient` bean for queue management
- Maintained the same queue name (`image-processing`) for compatibility

### 3. Code Changes

**Message Sending (Web Services):**
- Replaced `RabbitTemplate` with `ServiceBusTemplate`
- Changed `convertAndSend()` to `send()` with MessageBuilder pattern
- Updated imports to use Azure Service Bus classes

**Message Listening (Worker Service):**
- Replaced `@RabbitListener` with `@ServiceBusListener`
- Replaced RabbitMQ Channel with ServiceBus MessageContext
- Changed acknowledgment from `channel.basicAck()` to `context.complete()`
- Changed rejection from `channel.basicNack()` to `context.abandon()`

### 4. Application Properties

**Old RabbitMQ Configuration:**
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

**New Azure Service Bus Configuration:**
```properties
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}
```

## Environment Variables Required

To run the application with Azure Service Bus, set these environment variables:

```bash
export AZURE_CLIENT_ID=your-managed-identity-client-id
export SERVICE_BUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net
```

## Architecture

The migration maintains the same messaging architecture:
- Web module publishes `ImageProcessingMessage` to the queue
- Worker module consumes messages and processes thumbnail generation
- Same retry logic and error handling patterns
- Same message structure and queue name

## Benefits of Migration

1. **Managed Identity Authentication**: No need to manage connection strings or credentials
2. **Azure Integration**: Better integration with other Azure services
3. **Scalability**: Azure Service Bus provides enterprise-grade messaging
4. **Reliability**: Built-in dead letter queues and retry mechanisms
5. **Monitoring**: Integrated with Azure Monitor and Application Insights

## Testing

The migration has been tested for:
- ✅ Compilation success
- ✅ Dependency resolution
- ✅ Configuration syntax
- ✅ Message structure compatibility

## Rollback Plan

If needed, the migration can be rolled back by:
1. Reverting the dependency changes in POM files
2. Restoring the original RabbitConfig files
3. Updating service classes to use RabbitTemplate
4. Restoring RabbitMQ configuration in application.properties