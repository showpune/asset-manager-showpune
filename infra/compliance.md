# Infrastructure Compliance Report

**Project**: Asset Manager Kit - Azure Migration  
**IaC Tool**: Bicep  
**Deployment Tool**: Azure CLI (`az deployment group create`)  
**Generated**: Infrastructure Bicep Generation Task 001

---

## Security Controls

### Managed Identity (Zero Credential Architecture)

| Control | Implementation | Status |
|---------|---------------|--------|
| No hardcoded credentials | All services use user-assigned managed identity | ✅ |
| Principle of least privilege | Scoped RBAC roles per service | ✅ |
| Azure AD authentication for PostgreSQL | `activeDirectoryAuth: 'Enabled'` | ✅ |
| Managed identity as PostgreSQL AD admin | `postgresAdAdministrator` resource | ✅ |

### RBAC Role Assignments

| Service | Role | Scope |
|---------|------|-------|
| Azure Blob Storage | `Storage Blob Data Contributor` (ba92f5b4) | Storage Account |
| Azure Service Bus | `Azure Service Bus Data Owner` (090c5cfd) | Service Bus Namespace |
| Azure Database for PostgreSQL | AD Administrator (Service Principal) | PostgreSQL Server |

### Network Security

| Control | Implementation | Status |
|---------|---------------|--------|
| HTTPS only for storage | `supportsHttpsTrafficOnly: true` | ✅ |
| Minimum TLS 1.2 for storage | `minimumTlsVersion: 'TLS1_2'` | ✅ |
| No public blob access | `allowBlobPublicAccess: false` | ✅ |
| PostgreSQL allows Azure services only | Firewall rule `0.0.0.0`→`0.0.0.0` | ✅ |

### Data Protection

| Control | Implementation | Status |
|---------|---------------|--------|
| Storage encryption at rest | `Microsoft.Storage` managed keys | ✅ |
| Storage soft delete (7 days) | `deleteRetentionPolicy.days: 7` | ✅ |
| PostgreSQL automated backups | `backupRetentionDays: 7` | ✅ |
| Service Bus dead-letter queue | `deadLetteringOnMessageExpiration: true` | ✅ |

---

## Azure Well-Architected Framework Alignment

### Reliability

| Pillar | Control | Detail |
|--------|---------|--------|
| Reliability | Locally redundant storage | `Standard_LRS` - 3 copies within region |
| Reliability | Service Bus message TTL | 14-day message retention |
| Reliability | Dead-letter queue | Messages not processed after 3 attempts go to DLQ |
| Reliability | PostgreSQL backup | 7-day automated backup retention |

### Security

| Pillar | Control | Detail |
|--------|---------|--------|
| Security | Zero-trust identity | User-assigned managed identity, no shared secrets |
| Security | Azure AD authentication | PostgreSQL AD auth enabled alongside password auth |
| Security | Encryption in transit | TLS 1.2 minimum, HTTPS enforced |
| Security | Encryption at rest | Storage server-side encryption with Microsoft-managed keys |

### Cost Optimization

| Pillar | Control | Detail |
|--------|---------|--------|
| Cost | Storage tier | Hot access tier for frequently accessed assets |
| Cost | PostgreSQL tier | Burstable B2ms - cost-effective for variable workloads |
| Cost | Service Bus tier | Standard (not Premium) - suitable for non-production |
| Cost | No geo-redundancy | Geo-redundant backup disabled for dev environment |

### Operational Excellence

| Pillar | Control | Detail |
|--------|---------|--------|
| Ops | Parameterised templates | All environment-specific values are parameters |
| Ops | Unique resource naming | `uniqueString(resourceGroup().id)` prevents naming conflicts |
| Ops | Module decomposition | Separate module per resource type for maintainability |
| Ops | Deployment outputs | All connection values exported as deployment outputs |

---

## Deployment Tool Compliance

| Rule | Requirement | Status |
|------|-------------|--------|
| Use Azure CLI | `az deployment group create` used in scripts | ✅ |
| No azd | Azure Developer CLI (azd) not used | ✅ |
| Bicep modules | Reusable modules in `modules/` directory | ✅ |
| Parameters file | `parameters.json` for environment config | ✅ |

---

## Known Limitations and Recommendations

1. **PostgreSQL firewall**: The `AllowAllAzureIps` rule (`0.0.0.0`→`0.0.0.0`) allows any Azure-hosted service to reach the server. For production, restrict to specific virtual network subnet IDs using VNet integration.

2. **Storage public access**: `allowSharedKeyAccess: true` is retained for initial compatibility. After migrating all clients to Managed Identity, set this to `false`.

3. **Service Bus tier**: Standard tier is used; for production with high throughput requirements, consider upgrading to Premium for dedicated capacity and VNet integration.

4. **PostgreSQL HA**: High availability is disabled (`mode: 'Disabled'`). Enable zone-redundant HA for production workloads.

5. **Secret management**: `postgresAdminPassword` is a plain parameter. For production, use Azure Key Vault references in `parameters.json`.
