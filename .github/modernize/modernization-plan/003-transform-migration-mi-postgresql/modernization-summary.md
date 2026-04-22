# Modernization Summary: 003-transform-migration-mi-postgresql

## Task Description
Migrate PostgreSQL authentication from password-based to Azure Managed Identity in both the `web` and `worker` modules.

## Changes Made

### Root `pom.xml`
- Added `spring-cloud-azure-dependencies` BOM version `5.22.0` (compatible with Spring Boot 3.2.1) to `<dependencyManagement>`.
- Added `spring-cloud-azure.version` property.

### `web/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM).

### `worker/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM).

### `web/src/main/resources/application.properties`
- Removed `spring.datasource.password=postgres` and plaintext `spring.datasource.username=postgres`.
- Updated `spring.datasource.url` to use Azure Database for PostgreSQL format with environment variables (`${POSTGRESQL_SERVER}`, `${POSTGRESQL_PORT}`, `${POSTGRESQL_DATABASE}`).
- Added passwordless configuration:
  - `spring.datasource.username=${MANAGED_IDENTITY_NAME}`
  - `spring.datasource.azure.passwordless-enabled=true`
  - `spring.cloud.azure.credential.client-id=${MANAGED_IDENTITY_CLIENT_ID}`
  - `spring.cloud.azure.credential.managed-identity-enabled=true`
- Added comments for service principal auth and Azure sovereign cloud deployment.

### `worker/src/main/resources/application.properties`
- Same changes as web module above.

### `web/src/test/resources/application.properties`
- No changes — tests continue to use H2 in-memory database with its own credentials, unaffected by the PostgreSQL migration.

## Verification
- ✅ Build passes
- ✅ All unit tests pass
- ✅ No critical or major consistency issues
- ✅ No security vulnerabilities in added dependencies
