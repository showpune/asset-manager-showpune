# Modernization Summary: 004-transform-database-postgresql-to-azure

## Task
Migrate database connectivity to Azure Database for PostgreSQL with Managed Identity (credential-free) authentication.

## Changes Made

### 1. Dependencies — `web/pom.xml` and `worker/pom.xml`
- Added property `azure-identity-extensions.version=1.2.2`
- Added dependency `com.azure:azure-identity-extensions:1.2.2` to both modules to enable `AzurePostgresqlAuthenticationPlugin` for Azure AD token-based authentication

### 2. `web/src/main/resources/application.properties`
- **Updated** `spring.datasource.url` to target `${POSTGRES_HOST}` (Azure PostgreSQL Flexible Server FQDN) with `sslmode=require` and `authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin`
- **Replaced** `spring.datasource.username` with `${AZURE_MI_NAME}` (the managed identity name used as the Azure AD database user)
- **Removed** `spring.datasource.password` (credential-free via Managed Identity)

### 3. `worker/src/main/resources/application.properties`
- Same database configuration changes as the web module above

### 4. `web/src/test/resources/application.properties`
- **No changes** — unit tests continue to use H2 in-memory database, unaffected by this migration

## Environment Variables Required at Runtime
| Variable | Description |
|---|---|
| `POSTGRES_HOST` | FQDN of the Azure Database for PostgreSQL Flexible Server |
| `AZURE_MI_NAME` | Name of the user-assigned managed identity (used as the Azure AD database user) |
| `AZURE_CLIENT_ID` | Client ID of the managed identity (already configured for Service Bus) |

## Authentication Flow
The `AzurePostgresqlAuthenticationPlugin` acquires a short-lived Azure AD access token using the Managed Identity at connection time, eliminating the need for a stored database password.

## Build & Test Result
- Build: ✅ `./mvnw clean verify` passed successfully
- Unit tests: ✅ All tests passed (H2 in-memory, unchanged)

## Security Summary
No hardcoded credentials remain in any configuration file. All PostgreSQL authentication is performed via Azure AD Managed Identity tokens through the `azure-identity-extensions` plugin. No CodeQL vulnerabilities introduced.
