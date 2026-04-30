# Task 003 – Migrate PostgreSQL to Azure Managed Identity

## Summary

Migrated both `assets-manager-web` and `assets-manager-worker` modules from password-based PostgreSQL authentication to Azure Managed Identity for Azure Database for PostgreSQL using Spring Cloud Azure.

## Changes Made

### Parent `pom.xml`
- Added `spring-cloud-azure.version=5.22.0` property (compatible with Spring Boot 3.2.1)
- Added `spring-cloud-azure-dependencies` BOM in `<dependencyManagement>` for centralized version management

### `web/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM)

### `worker/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM)

### `web/src/main/resources/application.properties`
- Removed `spring.datasource.password` (hardcoded `postgres`)
- Updated `spring.datasource.url` to use Azure PostgreSQL format with environment variables (`${POSTGRESQL_SERVER}`, `${POSTGRESQL_PORT}`, `${POSTGRESQL_DATABASE}`) and `sslMode=REQUIRED`
- Updated `spring.datasource.username` to `${MANAGED_IDENTITY_NAME}`
- Added `spring.datasource.azure.passwordless-enabled=true`
- Added `spring.cloud.azure.credential.client-id=${MANAGED_IDENTITY_CLIENT_ID}`
- Added `spring.cloud.azure.credential.managed-identity-enabled=true`
- Added comments for sovereign cloud, service principal auth, and system-assigned MI

### `worker/src/main/resources/application.properties`
- Same changes as web module datasource configuration

## Consistency Check Results

- 0 Critical issues
- 0 Major issues
- 2 Minor issues fixed: `spring.cloud.azure.credential.client-id` changed from literal placeholder to `${MANAGED_IDENTITY_CLIENT_ID}` environment variable reference

## Build / Test Notes

The full project build has pre-existing compilation failures caused by incomplete parallel tasks:
- **Task 001** (`dev-s3-to-blob`): AWS SDK dependencies removed but some source files still reference S3 classes
- **Task 002** (`dev-rabbitmq-to-servicebus`): RabbitMQ dependencies moved to test scope but main source files still reference `@EnableRabbit`, `RabbitTemplate`, and `spring-retry` classes

These failures are **not introduced by this task**. The PostgreSQL migration changes are isolated to `pom.xml` files and `application.properties` files, which do not affect Java source compilation.

## Authentication Flow

Post-migration, both modules authenticate to Azure Database for PostgreSQL using:
1. **User-assigned Managed Identity** (default): Specify `MANAGED_IDENTITY_CLIENT_ID`
2. **System-assigned Managed Identity**: Omit `spring.cloud.azure.credential.client-id`
3. **Service Principal**: Remove `managed-identity-enabled`, add tenant-id, client-id, and client-secret
