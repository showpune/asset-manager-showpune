# Modernization Summary: 003-transform-migration-mi-postgresql

## Task
Migrate PostgreSQL from password-based authentication to Azure Managed Identity in both the web and worker modules.

## Changes Made

### 1. `pom.xml` (parent)
- Added `spring-cloud-azure-dependencies` BOM version `5.22.0` (compatible with Spring Boot 3.2.1) to `<dependencyManagement>` as an imported POM.
- Added `spring-cloud-azure.version` property set to `5.22.0`.

### 2. `web/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by the parent BOM).

### 3. `worker/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by the parent BOM).

### 4. `web/src/main/resources/application.properties`
- Removed `spring.datasource.password=postgres`.
- Updated `spring.datasource.url` to use Azure Database for PostgreSQL format with environment variables (`${POSTGRESQL_SERVER}`, `${POSTGRESQL_PORT}`, `${POSTGRESQL_DATABASE}`) and `sslMode=REQUIRED`.
- Updated `spring.datasource.username` to use `${MANAGED_IDENTITY_NAME}`.
- Added `spring.datasource.azure.passwordless-enabled=true`.
- Added `spring.cloud.azure.credential.client-id=${MANAGED_IDENTITY_CLIENT_ID}`.
- Added `spring.cloud.azure.credential.managed-identity-enabled=true`.
- Added comments explaining service principal auth and sovereign cloud configuration.

### 5. `worker/src/main/resources/application.properties`
- Same changes as the web module's `application.properties`.

### Unchanged
- `web/src/test/resources/application.properties` — intentionally left unchanged; it uses H2 in-memory database for tests and does not connect to PostgreSQL.

## Verification
- **Build**: ✅ `mvn clean test` passes (BUILD SUCCESS)
- **Tests**: ✅ 1 test in web module passes; worker has no tests
- **Consistency Check**: ✅ No Critical or Major issues found
