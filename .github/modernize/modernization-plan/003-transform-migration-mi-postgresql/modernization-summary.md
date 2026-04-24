# Task 003 – Migrate PostgreSQL to Azure Managed Identity

## Summary

Migrated PostgreSQL authentication in both the **web** and **worker** modules from hardcoded password-based credentials to Azure Managed Identity (passwordless), using `spring-cloud-azure-starter-jdbc-postgresql`.

---

## Changes

### `pom.xml` (root)
- Added `spring-cloud-azure-dependencies` BOM (`5.19.0`) to `dependencyManagement` for consistent version management across modules.

### `web/pom.xml` and `worker/pom.xml`
- Added `com.azure.spring:spring-cloud-azure-starter-jdbc-postgresql` (version managed by BOM).
- This starter provides Spring auto-configuration for Azure Database for PostgreSQL with automatic MI access-token acquisition and refresh via `DefaultAzureCredential`.

### `web/src/main/resources/application.properties`
- **Removed** `spring.datasource.username=postgres` (hardcoded credential)
- **Removed** `spring.datasource.password=postgres` (hardcoded credential)
- **Added** `spring.datasource.username=${SPRING_DATASOURCE_USERNAME}` (externalized to environment variable)
- **Added** `spring.datasource.azure.passwordless-enabled=true`

### `worker/src/main/resources/application.properties`
- Same changes as web module.

---

## Authentication Flow

1. At runtime, `spring-cloud-azure-starter-jdbc-postgresql` detects `spring.datasource.azure.passwordless-enabled=true`.
2. It intercepts DataSource creation and uses `DefaultAzureCredential` to acquire an AAD access token for the scope `https://ossrdbms-aad.database.windows.net/.default`.
3. The access token is used as the JDBC password; the MI PostgreSQL role name is supplied via `SPRING_DATASOURCE_USERNAME` environment variable.
4. Token refresh is handled automatically by the Spring Cloud Azure auto-configuration before expiry.

---

## Test Impact

- Web module tests continue to use H2 in-memory database (`web/src/test/resources/application.properties`), which overrides the main datasource configuration and does not activate the passwordless auto-config.
- Worker module has no tests; no test changes required.
- **Build**: ✅ PASS  **Unit Tests**: ✅ PASS (1 test, 0 failures)
