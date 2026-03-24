# Infrastructure Compliance Report

## Task: 001-infrastructure-bicep-generation

**Generated:** Bicep IaC templates for Azure infrastructure provisioning  
**Deployment Tool:** Azure CLI (`az deployment group create`)  
**Provision:** Not provisioned (templates generated only)

---

## Requirements Coverage

| Requirement | Status | Implementation |
|-------------|--------|---------------|
| Azure Blob Storage account for asset storage | ✅ | `modules/storage.bicep` – StorageV2 account with `assets` blob container |
| Azure Service Bus namespace with queues | ✅ | `modules/servicebus.bicep` – Standard namespace with `image-processing` queue |
| Azure Database for PostgreSQL flexible server | ✅ | `modules/postgresql.bicep` – Flexible Server v14, `assets_manager` database |
| Managed Identity for all services | ✅ | `modules/identity.bicep` – User-assigned identity with RBAC on Storage and Service Bus |
| Parameters and outputs for service integration | ✅ | `main.bicep` – Full parameter set and 12 deployment outputs |

---

## Security Controls

### Identity & Access Management
- **User-Assigned Managed Identity** created and assigned to the PostgreSQL server; designed to be assigned to compute resources (App Service, Container Apps, AKS) at deploy time.
- **No credentials in templates** – `postgresAdminPassword` is a `@secure()` parameter never stored in `parameters.json`.
- **RBAC least-privilege** – managed identity is granted only the roles it needs:
  - `Storage Blob Data Contributor` (not Storage Account Contributor)
  - `Azure Service Bus Data Owner` (scoped to the namespace)

### Network & Transport Security
- Storage account: `minimumTlsVersion: TLS1_2`, `supportsHttpsTrafficOnly: true`, `allowBlobPublicAccess: false`
- Blob container: `publicAccess: None`
- Service Bus: connections require TLS (enforced by the service)
- PostgreSQL: `AllowAzureServices` firewall rule enables Azure-internal connectivity without exposing a public IP range; for production, replace with VNet integration

### Data Protection
- Storage: server-side encryption with Microsoft-managed keys enabled by default
- Storage: soft-delete enabled (7-day retention) on blob service
- PostgreSQL: automated backups enabled (7-day retention)

---

## Azure Well-Architected Framework Alignment

### Reliability
- PostgreSQL storage provisioned at 32 GB with 7-day backup retention
- Service Bus queue configured with dead-lettering on message expiration
- Service Bus `maxDeliveryCount: 3` matches worker retry policy

### Security
- All secrets passed at deployment time, never committed to source control
- Managed identity eliminates long-lived credentials
- Both password and Azure AD authentication enabled on PostgreSQL for a phased migration path

### Cost Optimization
- Default SKUs target development workloads:
  - Storage: `Standard_LRS` (single-region)
  - Service Bus: `Standard` tier
  - PostgreSQL: `Standard_B2s` / Burstable (low-cost dev tier)
- Production deployments should override with `Standard_GRS`, `Standard_D2s_v3 / GeneralPurpose`

### Operational Excellence
- All resources tagged with `environmentName` via naming convention
- Consistent naming pattern: `{projectName}-{environment}-{type}`
- `uniqueString()` used for storage account to ensure global uniqueness

---

## Deviations / Known Limitations

| Item | Notes |
|------|-------|
| Managed identity compute assignment | The managed identity is created and granted RBAC roles, but must be manually assigned to compute resources (App Service, Container Apps, VM) after the application hosting layer is provisioned. |
| PostgreSQL VNet integration | The `AllowAzureServices` firewall rule is sufficient for development. For production, replace with VNet-integrated deployment using `Microsoft.DBforPostgreSQL/flexibleServers` with `delegatedSubnetResourceId`. |
| Service Bus Premium features | Some features (VNet service endpoints, message sessions) require the `Premium` tier. Override `serviceBusSku=Premium` for production. |
| Azure AD PostgreSQL admin | `authConfig.activeDirectoryAuth` is enabled; to configure an AAD admin user, use `az postgres flexible-server ad-admin create` after deployment. |
