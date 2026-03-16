# Infrastructure Compliance Report

**Project**: assets-manager  
**IaC Tool**: Bicep + Azure CLI  
**Generated**: 2025

---

## Deployment Tool Compliance

| Rule | Status | Notes |
|---|---|---|
| Use `az deployment` (not `azd`) | ✅ | `deploy.sh` and `deploy.ps1` use `az deployment group create` |
| Bicep templates validated with `az bicep build` | ✅ | Validation step included in both deployment scripts |
| No hard-coded secrets in templates | ✅ | `postgresAdminPassword` is `@secure()` parameter; never appears in `parameters.json` |

---

## Security Baseline

| Control | Status | Resource | Details |
|---|---|---|---|
| Managed Identity for service authentication | ✅ | All services | User-Assigned Managed Identity with scoped RBAC role assignments |
| No storage account public blob access | ✅ | Storage Account | `allowBlobPublicAccess: false` |
| HTTPS-only traffic for storage | ✅ | Storage Account | `supportsHttpsTrafficOnly: true` |
| Minimum TLS 1.2 for storage | ✅ | Storage Account | `minimumTlsVersion: 'TLS1_2'` |
| Minimum TLS 1.2 for Service Bus | ✅ | Service Bus | `minimumTlsVersion: '1.2'` |
| PostgreSQL AAD authentication enabled | ✅ | PostgreSQL | `activeDirectoryAuthEnabled: 'Enabled'` |
| PostgreSQL SSL required | ✅ | PostgreSQL | Enforced by default in Flexible Server; JDBC URL uses `sslmode=require` |
| Blob soft delete enabled | ✅ | Storage Account | Blob and container soft delete with 7-day retention |
| Service Bus dead-letter queue | ✅ | Service Bus Queue | `deadLetteringOnMessageExpiration: true` |
| Resource tagging | ✅ | All resources | Tags: `application`, `environment`, `managedBy` |

---

## RBAC Role Assignments

| Principal | Scope | Role | Role ID |
|---|---|---|---|
| Managed Identity | Storage Account | Storage Blob Data Contributor | `ba92f5b4-2d11-453d-a403-e96b0029c9fe` |
| Managed Identity | Service Bus Namespace | Azure Service Bus Data Owner | `090c5cfd-751d-490a-894a-3ce6f1109419` |
| Managed Identity | PostgreSQL Server | AAD Administrator (ServicePrincipal) | via `administrators` resource |

---

## Known Deviations and Recommendations

| Item | Severity | Recommendation |
|---|---|---|
| Single Managed Identity for web and worker | Low | For stricter least-privilege, create two identities: assign **Service Bus Data Sender** to web and **Service Bus Data Receiver** to worker. |
| PostgreSQL public network access with Azure Services firewall rule | Medium | In production, restrict firewall to known IP ranges or deploy within a VNet with Private Endpoint. |
| Password auth enabled on PostgreSQL | Low | Once the application is migrated to `DefaultAzureCredential`, disable password auth: set `passwordAuthEnabled: 'Disabled'`. |
| Service Bus `disableLocalAuth: false` | Low | Set `disableLocalAuth: true` after confirming all clients use Managed Identity to eliminate shared-key access. |
| Storage `allowSharedKeyAccess: true` | Low | Set to `false` once the application is confirmed to use `DefaultAzureCredential` exclusively. |
| No VNet integration | Medium | For production, deploy all services within a VNet and use Private Endpoints to restrict public exposure. |
| No Azure Monitor / Diagnostic Settings | Low | Add diagnostic settings to stream logs and metrics to a Log Analytics Workspace. |

---

## Resource Naming Convention

| Resource | Pattern | Example |
|---|---|---|
| Managed Identity | `id-{appName}-{env}` | `id-assets-manager-dev` |
| Storage Account | `st{8-char-appName}{env}{6-char-unique}` | `stassetsmadev3f8a2c` |
| Service Bus Namespace | `sb-{appName}-{env}-{6-char-unique}` | `sb-assets-manager-dev-3f8a2c` |
| PostgreSQL Server | `psql-{appName}-{env}-{6-char-unique}` | `psql-assets-manager-dev-3f8a2c` |

Names incorporate `uniqueString(resourceGroup().id)` to ensure global uniqueness
while remaining deterministic for the same resource group.
