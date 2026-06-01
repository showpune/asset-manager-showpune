# Modernization Summary: 003-transform-migration-mi-postgresql

## Task Description
Migrate PostgreSQL from password-based authentication to Azure Managed Identity in both the web and worker modules.

## Changes Made

### Root `pom.xml`
- Added `spring-cloud-azure.version` property set to `5.22.0` (compatible with Spring Boot 3.x).
- Added `dependencyManagement` block importing the `spring-cloud-azure-dependencies` BOM.

### `web/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM).

### `worker/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM).

### `web/src/main/resources/application.properties`
- Removed `spring.datasource.username=postgres` and `spring.datasource.password=postgres`.
- Updated `spring.datasource.url` to use Azure Database for PostgreSQL format with environment variable references (`${POSTGRESQL_SERVER}`, `${POSTGRESQL_PORT}`, `${POSTGRESQL_DATABASE}`).
- Set `spring.datasource.username` to `${MANAGED_IDENTITY_NAME}`.
- Added `spring.datasource.azure.passwordless-enabled=true`.
- Added `spring.cloud.azure.credential.client-id=${MANAGED_IDENTITY_CLIENT_ID}`.
- Added `spring.cloud.azure.credential.managed-identity-enabled=true`.
- Added guidance comments for service principal auth and Azure sovereign cloud configuration.

### `worker/src/main/resources/application.properties`
- Same changes as the web module above.

### `web/src/test/resources/application.properties`
- **Not changed** — uses H2 in-memory database for tests, which does not involve PostgreSQL credentials.

## Outcome
- ✅ Build passes
- ✅ All unit tests pass
- ✅ No plaintext PostgreSQL credentials remain in any committed configuration
- ✅ Both modules use Azure Managed Identity for PostgreSQL authentication
