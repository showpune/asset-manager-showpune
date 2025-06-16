# Azure Service Bus Migration

This document outlines the migration from RabbitMQ to Azure Service Bus.

## Changes Made

### Dependencies
- Replaced `spring-boot-starter-amqp` with Azure Service Bus dependencies:
  - `spring-cloud-azure-starter`
  - `spring-messaging-azure-servicebus`
- Added Spring Retry for the worker module

### Configuration
- **RabbitConfig** → **ServiceBusConfig**
- Uses Azure Managed Identity for authentication
- Automatically creates queue "image-processing" if it doesn't exist

### Application Properties
```properties
# Azure Service Bus Configuration
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=${AZURE_CLIENT_ID:test-client-id}
spring.cloud.azure.servicebus.entity-type=queue
spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE:test-namespace.servicebus.windows.net}
```

### Code Changes
- **Message Publishing**: `RabbitTemplate.convertAndSend()` → `ServiceBusTemplate.send()`
- **Message Consuming**: `@RabbitListener` → `@ServiceBusListener`
- **Application Setup**: `@EnableRabbit` → `@EnableAzureMessaging`
- **Message Acknowledgment**: Manual channel acknowledgment → Automatic framework handling

### Environment Variables Required
- `AZURE_CLIENT_ID`: Azure client ID for managed identity
- `SERVICE_BUS_NAMESPACE`: Azure Service Bus namespace (e.g., `your-namespace.servicebus.windows.net`)

### Testing
- Added test configuration with mocked Azure Service Bus components
- Tests run with `@ActiveProfiles("test")` to disable actual Service Bus connections

## Migration Benefits
- **Managed Service**: Azure Service Bus is fully managed, eliminating infrastructure maintenance
- **Built-in Security**: Uses Azure Managed Identity instead of connection strings
- **Enterprise Features**: Built-in dead lettering, message sessions, and retry policies
- **Scalability**: Auto-scaling capabilities with Azure
- **Monitoring**: Integrated with Azure Monitor and Application Insights

## Backward Compatibility
The migration maintains the same message structure (`ImageProcessingMessage`) and processing logic, ensuring seamless transition between RabbitMQ and Azure Service Bus.