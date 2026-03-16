# Task 004: Migrate Database Connectivity to Azure Database for PostgreSQL

## Summary

Migrated both `web` and `worker` modules to use Azure Database for PostgreSQL Flexible Server with Managed Identity (passwordless) authentication via `AzurePostgresqlAuthenticationPlugin`.

## Changes Made

### 1. `web/pom.xml` and `worker/pom.xml`
- Added `com.azure:azure-identity-extensions:1.2.2` dependency to both modules to enable the `AzurePostgresqlAuthenticationPlugin` for managed identity authentication.

### 2. `web/src/main/resources/application.properties`
- Updated `spring.datasource.url` to connect to Azure PostgreSQL Flexible Server using environment variable `${AZURE_POSTGRESQL_HOST}`, with `sslmode=require` and `authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin`.
- Replaced `spring.datasource.username` with `${AZURE_MI_NAME}` (the user-assigned managed identity name used as the AAD admin).
- Removed `spring.datasource.password` (credential-free authentication via Managed Identity).

### 3. `worker/src/main/resources/application.properties`
- Same JDBC URL, username, and password changes as the web module.

## Environment Variables Required

| Variable | Description |
|---|---|
| `AZURE_POSTGRESQL_HOST` | Fully qualified domain name of the Azure PostgreSQL Flexible Server |
| `AZURE_MI_NAME` | Name of the user-assigned managed identity (e.g. `id-assets-manager-dev`) used as AAD admin |
| `AZURE_CLIENT_ID` | Client ID of the managed identity (already configured for other Azure services) |

## Build & Test Results

- Build: **PASSED**
- Unit Tests: **PASSED** (H2 in-memory database used for tests, unaffected by this change)
