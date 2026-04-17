# Modernization Summary: 003-transform-rabbitmq-to-azure-service-bus

## Task
Migrate asynchronous messaging from RabbitMQ (AMQP) to Azure Service Bus.

## Changes Made

### Build Files
| File | Change |
|------|--------|
| `pom.xml` | Added `spring-cloud-azure-dependencies` BOM (5.22.0) to `dependencyManagement` |
| `web/pom.xml` | Removed `spring-boot-starter-amqp`; added `spring-cloud-azure-starter` and `spring-messaging-azure-servicebus` |
| `worker/pom.xml` | Same as web; additionally added explicit `spring-retry` dependency |

### Configuration Files
| File | Change |
|------|--------|
| `web/src/main/resources/application.properties` | Removed `spring.rabbitmq.*` properties; added `spring.cloud.azure.credential.managed-identity-enabled`, `spring.cloud.azure.credential.client-id`, `spring.cloud.azure.servicebus.namespace`, `spring.cloud.azure.servicebus.entity-type` |
| `web/src/test/resources/application.properties` | Added Azure Service Bus test namespace; disabled managed identity for tests |
| `worker/src/main/resources/application.properties` | Same RabbitMQ → Azure Service Bus property migration |

### Java Source Files
| File | Change |
|------|--------|
| `web/.../AssetsManagerApplication.java` | Removed `@EnableRabbit` annotation |
| `worker/.../WorkerApplication.java` | Removed `@EnableRabbit` annotation |
| `web/.../config/RabbitConfig.java` → `ServiceBusConfig.java` | Renamed class; replaced AMQP beans (`Queue`, `MessageConverter`, `SimpleRabbitListenerContainerFactory`) with Azure Service Bus beans (`ServiceBusAdministrationClient`, `QueueProperties`, `PropertiesSupplier<ConsumerIdentifier, ProcessorProperties>` for manual ack); added `@EnableAzureMessaging` |
| `worker/.../config/RabbitConfig.java` → `ServiceBusConfig.java` | Same as web config; retained `RetryTemplate` bean for 3-attempt / 60-second backoff retry logic |
| `web/.../service/BackupMessageProcessor.java` | Replaced `@RabbitListener` + `Channel` + `AmqpHeaders.DELIVERY_TAG` with `@ServiceBusListener` + `ServiceBusReceivedMessageContext`; `basicAck` → `context.complete()`, `basicNack` → `context.abandon()` |
| `worker/.../service/AbstractFileProcessingService.java` | Same listener migration; preserved retry logic via `RetryTemplate`; renamed outer context parameter to `serviceBusContext` to avoid variable shadowing |
| `web/.../service/AwsS3Service.java` | Replaced `RabbitTemplate.convertAndSend` with `ServiceBusTemplate.send` using `MessageBuilder.withPayload` |
| `web/.../service/LocalFileStorageService.java` | Same publisher migration as `AwsS3Service` |

### Scripts
| File | Change |
|------|--------|
| `scripts/start.sh` | Removed RabbitMQ Docker container startup |
| `scripts/stop.sh` | Removed RabbitMQ Docker container teardown |
| `scripts/start.cmd` | Removed RabbitMQ Docker container startup |
| `scripts/stop.cmd` | Removed RabbitMQ Docker container teardown |

## Key Design Decisions
- **Managed Identity**: Production auth uses managed identity (`spring.cloud.azure.credential.managed-identity-enabled=true`); test environment disables it to avoid connection attempts
- **Manual Acknowledgment**: `ProcessorProperties.setAutoComplete(false)` preserves original MANUAL ack mode; `context.complete()` replaces `basicAck`, `context.abandon()` replaces `basicNack`
- **Retry Logic**: Original retry behavior (max 3 attempts, 60-second backoff) preserved in `RetryTemplate` bean in `ServiceBusConfig`; the `@ServiceBusListener` processor's autoComplete is disabled to allow manual retry control
- **Queue Provisioning**: `ServiceBusAdministrationClient` creates/verifies the `image-processing` queue on startup (only when managed identity is enabled via `@ConditionalOnProperty`)
- **`ServiceBusConfig` naming**: Renamed from `RabbitConfig` to `ServiceBusConfig` to fully remove old technology references

## Success Criteria
- ✅ Build passes
- ✅ Unit tests pass (1 test, 0 failures)
- ✅ No RabbitMQ/AMQP references remain in source, config, or build files
