# IaC Rules Compliance Report

## Applied Rules

### General Bicep Rules

| Rule | Status | Implementation |
|---|---|---|
| Resource token format: `uniqueString(subscription().id, resourceGroup().id, location, environmentName)` (scope = resourceGroup) | ✅ Applied | Each module calculates `var resourceToken = uniqueString(subscription().id, resourceGroup().id, location, environmentName)` |
| All resources named `az{resourcePrefix}{resourceToken}` (prefix ≤ 3 chars, alphanumeric only) | ✅ Applied | `azmi`, `azst`, `azsb`, `azpg` prefixes used across all modules |
| Expected files: `main.bicep`, `main.parameters.json` | ✅ Applied | Both files present |
| Must call `appmod-get-available-region-sku` to determine available regions/SKUs | ✅ Applied | Called before generating templates |

### Deployment Tool (Azure CLI)

| Rule | Status | Implementation |
|---|---|---|
| Use `.ps1` extension for PowerShell scripts | ✅ Applied | `deploy.ps1` |
| Use `.sh` extension for Bash scripts | ✅ Applied | `deploy.sh` |
| Ensure all steps execute successfully; fix and rerun on failure | ✅ Applied | Both scripts use `$ErrorActionPreference = "Stop"` / `set -euo pipefail`; each step checks `$LASTEXITCODE` / exit status |
| Validate PowerShell script syntax (brace matching, string termination) | ✅ Applied | Script validated for proper PowerShell syntax |

### Azure Storage Account

| Rule | Status | Implementation |
|---|---|---|
| Disable storage account local auth (key access) | ✅ Applied | `allowSharedKeyAccess: false` in `modules/storage.bicep` |
| Disable storage account anonymous blob access | ✅ Applied | `allowBlobPublicAccess: false` in `modules/storage.bicep`; container `publicAccess: 'None'` |

### Azure Service Bus

| Rule | Status | Implementation |
|---|---|---|
| No additional IaC rules for Service Bus | ✅ N/A | Standard SKU provisioned as required by the plan |

### Azure Database for PostgreSQL

| Rule | Status | Implementation |
|---|---|---|
| Use PostgreSQL version `17` or higher | ✅ Applied | `version: '17'` in `modules/postgresql.bicep` |
| Do not create a database named `postgres` | ✅ Applied | Database named `asset_manager` |
| Add firewall rule to allow traffic from Azure Services (0.0.0.0) | ✅ Applied | `firewallAllowAzureServices` rule with `startIpAddress: '0.0.0.0'`, `endIpAddress: '0.0.0.0'` |
| App uses Managed Identity → add post-provision Service Connector step | ✅ Applied | Documented in both deployment scripts and README.md under "Post-Deployment — Service Connector" |
| Service Connector: use `--user-identity client-id=XX subs-id=XX` | ✅ Applied | Scripts include the `--user-identity` parameter; no `--system-identity` used |
| Service Connector: use `--client-type springBoot` | ✅ Applied | Scripts specify `--client-type springBoot` |

### Key Vault

| Rule | Status | Implementation |
|---|---|---|
| Use Key Vault only when application has secrets to store | ✅ Skipped | App uses Managed Identity for all services (no secrets); Key Vault not provisioned |

## Resources Summary

| Resource | Module | Naming | Region |
|---|---|---|---|
| User-Assigned Managed Identity | `modules/identity.bicep` | `azmi<token>` | `location` param |
| Storage Account | `modules/storage.bicep` | `azst<token>` | `location` param |
| Blob Container (`assets`) | `modules/storage.bicep` | Fixed: `assets` | N/A |
| RBAC: Storage Blob Data Contributor | `modules/storage.bicep` | GUID-based | N/A |
| Service Bus Namespace (Standard) | `modules/servicebus.bicep` | `azsb<token>` | `location` param |
| Service Bus Queue (`image-processing`) | `modules/servicebus.bicep` | Fixed: `image-processing` | N/A |
| RBAC: Service Bus Data Owner | `modules/servicebus.bicep` | GUID-based | N/A |
| PostgreSQL Flexible Server (B1ms) | `modules/postgresql.bicep` | `azpg<token>` | `location` param |
| PostgreSQL Database (`asset_manager`) | `modules/postgresql.bicep` | Fixed: `asset_manager` | N/A |
| PostgreSQL Firewall (AllowAzureServices) | `modules/postgresql.bicep` | Fixed: `AllowAzureServices` | N/A |
| PostgreSQL Entra Admin (MI) | `modules/postgresql.bicep` | `<identityPrincipalId>` | N/A |
