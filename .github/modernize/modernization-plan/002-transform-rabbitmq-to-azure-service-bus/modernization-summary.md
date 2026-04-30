# Task 002: Migrate RabbitMQ to Azure Service Bus

## Summary

Migrated both `assets-manager-web` and `assets-manager-worker` modules from RabbitMQ AMQP messaging to Azure Service Bus via Spring Messaging, using Managed Identity authentication.

## Changes Made

### Dependencies (`web/pom.xml`, `worker/pom.xml`)
- **web**: Moved `spring-boot-starter-amqp` to `test` scope (required by frozen baseline tests that import `RabbitAutoConfiguration`)
- **worker**: Removed `spring-boot-starter-amqp` entirely; added `org.springframework.retry:spring-retry` explicitly (previously transitive via AMQP)
- Both modules: Added `com.azure.spring:spring-cloud-azure-starter` and `com.azure.spring:spring-messaging-azure-servicebus`
- Removed duplicate `spring-cloud-azure-dependencies` BOM from child poms (parent already manages it via task 003)

### Configuration (`web/config/RabbitConfig.java`, `worker/config/RabbitConfig.java`)
- Replaced `Queue` bean → `QueueProperties` bean (queue topology, no Exchange/Binding)
- Replaced `Jackson2JsonMessageConverter` bean → removed (auto-configured, no customization needed)
- Replaced `SimpleRabbitListenerContainerFactory` → `PropertiesSupplier<ConsumerIdentifier, ProcessorProperties>` with `autoComplete(false)` for manual acknowledgement
- Added `ServiceBusAdministrationClient` bean using `AzureServiceBusProperties` + `TokenCredential`
- Added `@EnableAzureMessaging` on configuration class
- `worker/RabbitConfig.java`: Retained `RetryTemplate` bean (used by `AbstractFileProcessingService` for retry logic)

### Message Producer (`web/service/AzureBlobStorageService.java`)
- Replaced `RabbitTemplate` injection → `ServiceBusTemplate`
- Replaced `rabbitTemplate.convertAndSend(QUEUE_NAME, message)` → `serviceBusTemplate.send(QUEUE_NAME, MessageBuilder.withPayload(message).build())`
- Fixed pre-existing N+1 query in `listObjects()`: moved `imageMetadataRepository.findAll()` outside stream to a `Map<String, Instant>` lookup

### Message Consumer (`web/service/BackupMessageProcessor.java`)
- `@RabbitListener(queues = QUEUE_NAME)` → `@ServiceBusListener(destination = QUEUE_NAME)`
- `Channel channel` + `long deliveryTag` → `@Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context`
- `channel.basicAck(...)` → `context.complete()` (with null-check)
- `channel.basicNack(...)` → `context.abandon()` (with null-check)
- Removed all AMQP imports

### Message Consumer (`worker/service/AbstractFileProcessingService.java`)
- Same `@RabbitListener` → `@ServiceBusListener` migration as BackupMessageProcessor
- `retryTemplate` field retained for `WorkerBaselineIT` reflection-based tests

### Application Entry Points
- `worker/WorkerApplication.java`: Removed `@EnableRabbit` annotation

### Application Properties (`web/src/main/resources/application.properties`, `worker/src/main/resources/application.properties`)
- Removed `spring.rabbitmq.*` properties
- Added `spring.cloud.azure.servicebus.namespace=${SERVICE_BUS_NAMESPACE}`
- Added `spring.cloud.azure.servicebus.entity-type=queue`

## Verification

- **Build**: `mvn clean compile` — SUCCESS
- **Unit tests**: `mvn test` — 1 test in web module passes, 0 in worker (no unit tests present). BUILD SUCCESS.
- **Consistency check**: Zero Critical issues. Zero Major issues after fixing N+1 query in `listObjects()`. Minor issues noted (variable naming conventions, `azure-identity` version pinned).

## Baseline Test Compatibility

- `spring-boot-starter-amqp` retained as `test` scope in `web/pom.xml` so frozen `WebBaselineIT`/`BaselineTestConfig` (which import `RabbitAutoConfiguration`) continue to compile.
- `@Profile("!baseline")` on `RabbitConfig` (pre-existing from task 000) ensures Service Bus beans do not load during baseline tests.
- `retryTemplate` field in `AbstractFileProcessingService` retained for `WorkerBaselineIT` reflection injection.
