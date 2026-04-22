# Modernization Summary: 003-transform-migration-mi-postgresql

## Task
Migrate PostgreSQL from password-based authentication to Azure Managed Identity in both the web and worker modules.

## Changes Made

### 1. Root `pom.xml`
- Added `spring-cloud-azure.version` property set to `5.22.0` (compatible with Spring Boot 3.2.1)
- Added `spring-cloud-azure-dependencies` BOM in `dependencyManagement` to centrally manage Spring Cloud Azure versions

### 2. `web/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM)

### 3. `worker/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` dependency (version managed by BOM)

### 4. `web/src/main/resources/application.properties`
- Removed `spring.datasource.password=postgres` (plaintext credential)
- Updated `spring.datasource.url` to use Azure Database for PostgreSQL format with environment variables (`${POSTGRESQL_SERVER}`, `${POSTGRESQL_PORT}`, `${POSTGRESQL_DATABASE}`)
- Updated `spring.datasource.username` to use `${MANAGED_IDENTITY_NAME}`
- Added `spring.datasource.azure.passwordless-enabled=true`
- Added `spring.cloud.azure.credential.managed-identity-enabled=true`
- Added `spring.cloud.azure.credential.client-id` placeholder
- Added comments for service principal auth and Azure sovereign cloud deployment

### 5. `worker/src/main/resources/application.properties`
- Same changes as web module (remove password, update URL/username, add passwordless properties and comments)

## Verification
- ✅ Build: `mvn clean test` passes successfully
- ✅ Tests: All 1 unit test in web module passes (H2 in-memory DB used in tests, unaffected by changes)
- ✅ Consistency check: No Critical or Major issues found
- ✅ No plaintext PostgreSQL credentials remain in any `application.properties`
