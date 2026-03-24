# Infrastructure Compliance Report

## Deployment Tool

All deployment scripts use **Azure CLI** (`az deployment group create / what-if`). Azure Developer CLI (`azd`) is **not** used.

---

## Resource Compliance

### Azure Blob Storage

| Rule | Status | Details |
|---|---|---|
| Managed Identity authentication | ✅ | `allowSharedKeyAccess: false` — shared key access disabled |
| HTTPS only | ✅ | `supportsHttpsTrafficOnly: true` |
| Minimum TLS version | ✅ | `minimumTlsVersion: TLS1_2` |
| Public blob access disabled | ✅ | `allowBlobPublicAccess: false` |
| Encryption at rest | ✅ | Microsoft-managed keys enabled for blob and file services |
| Soft delete for blobs | ✅ | `deleteRetentionPolicy.days: 7` |
| RBAC role assignments | ✅ | `Storage Blob Data Contributor` granted to web and worker managed identities |

### Azure Service Bus

| Rule | Status | Details |
|---|---|---|
| Managed Identity authentication | ✅ | `disableLocalAuth: true` — SAS key auth disabled |
| Minimum TLS version | ✅ | `minimumTlsVersion: 1.2` |
| Dead-letter queue | ✅ | `deadLetteringOnMessageExpiration: true` on image-processing queue |
| Message lock duration | ✅ | `lockDuration: PT5M` (5 minutes) |
| Max delivery count | ✅ | `maxDeliveryCount: 10` |
| RBAC role assignments | ✅ | Web app: Sender + Receiver; Worker: Receiver |

### Azure Database for PostgreSQL Flexible Server

| Rule | Status | Details |
|---|---|---|
| Microsoft Entra authentication | ✅ | `activeDirectoryAuth: Enabled` |
| Password auth (transitional) | ✅ | `passwordAuth: Enabled` (for initial admin setup) |
| Microsoft Entra admin configured | ✅ | Web app managed identity set as AAD admin |
| Backup retention | ✅ | `backupRetentionDays: 7` |
| Encryption at rest | ✅ | Enabled by default on Azure PostgreSQL Flexible Server |
| Firewall: Azure services only | ✅ | `AllowAzureServices` rule (0.0.0.0 → 0.0.0.0) |

### Managed Identities

| Rule | Status | Details |
|---|---|---|
| User-assigned identities | ✅ | Separate identities for web and worker modules |
| Principle of least privilege | ✅ | Web app: Sender+Receiver on SB, Blob Contributor on Storage; Worker: Receiver on SB, Blob Contributor on Storage |
| No hardcoded credentials | ✅ | All authentication via Managed Identity; PostgreSQL password referenced via Key Vault |

---

## General Rules

| Rule | Status | Details |
|---|---|---|
| All resources tagged | ✅ | `environment`, `application`, `managedBy` tags on all resources |
| Unique resource names | ✅ | `uniqueString(resourceGroup().id)` suffix prevents naming collisions |
| Parameters for environment differences | ✅ | SKU, size, location, environment name all parameterized |
| Secure parameters | ✅ | `postgresAdminPassword` is `@secure()` and Key Vault referenced in parameters.json |
| Modular structure | ✅ | One module per resource type under `modules/` |
| Outputs for downstream consumption | ✅ | All integration endpoints and identity client IDs exported |

---

## Known Limitations / Future Improvements

1. **PostgreSQL passwordless authentication for worker**: The worker module currently requires manual setup of the Entra user in PostgreSQL (`CREATE USER ... WITH LOGIN`). This can be automated with a deployment script post-provision step.
2. **VNet integration**: The current template uses public endpoints with Azure service firewall rules. For production, consider enabling VNet integration and private endpoints.
3. **Key Vault**: The `parameters.json` references a Key Vault for the PostgreSQL password. The Key Vault must be provisioned separately before first deployment.
4. **Service Bus Premium tier**: For production workloads requiring VNet integration or message sessions, consider upgrading to `Premium` tier.
