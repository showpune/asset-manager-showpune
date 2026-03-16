# Infrastructure Compliance Report – Asset Manager Kit

## Deployment Tool
- **Tool**: Azure CLI (`az deployment group create`)
- **IaC Language**: Bicep

---

## Resource Compliance Summary

### User-Assigned Managed Identity (`modules/identity.bicep`)
| Rule | Status | Notes |
|---|---|---|
| Use Managed Identity for service authentication | ✅ Pass | Single user-assigned identity shared across all resources |
| No credentials stored in templates | ✅ Pass | Identity is credential-free by design |

### Azure Blob Storage (`modules/storage.bicep`)
| Rule | Status | Notes |
|---|---|---|
| Public blob access disabled | ✅ Pass | `allowBlobPublicAccess: false` |
| Minimum TLS 1.2 enforced | ✅ Pass | `minimumTlsVersion: 'TLS1_2'` |
| HTTPS-only traffic | ✅ Pass | `supportsHttpsTrafficOnly: true` |
| Managed Identity RBAC (least privilege) | ✅ Pass | Storage Blob Data Contributor role |
| No hardcoded connection strings | ✅ Pass | SDK uses DefaultAzureCredential / managed identity |

### Azure Service Bus (`modules/servicebus.bicep`)
| Rule | Status | Notes |
|---|---|---|
| Minimum TLS 1.2 enforced | ✅ Pass | `minimumTlsVersion: '1.2'` |
| Managed Identity RBAC (least privilege) | ✅ Pass | Azure Service Bus Data Owner role |
| Dead-lettering enabled | ✅ Pass | `deadLetteringOnMessageExpiration: true` |
| No hardcoded connection strings | ✅ Pass | JMS client uses managed identity token |

### Azure Database for PostgreSQL Flexible Server (`modules/postgresql.bicep`)
| Rule | Status | Notes |
|---|---|---|
| SSL enforced | ✅ Pass | JDBC URL includes `sslmode=require` |
| Azure AD authentication enabled | ✅ Pass | `activeDirectoryAuth: 'Enabled'` |
| Managed Identity registered as Entra admin | ✅ Pass | Passwordless JDBC via Azure AD plugin |
| No credentials in application code | ✅ Pass | Azure AD token replaces password at runtime |
| Backup retention configured | ✅ Pass | 7-day backup retention |

---

## Security Recommendations for Production

1. **Restrict PostgreSQL firewall**: Replace the `AllowAzureServices` (0.0.0.0–0.0.0.0) rule with a VNet integration or specific IP allowlist.
2. **Disable PostgreSQL password authentication**: Set `passwordAuth: 'Disabled'` after completing Entra AD setup to enforce credential-free access.
3. **Enable Storage Account network rules**: Restrict the storage account to a specific VNet or IP range rather than `defaultAction: 'Allow'`.
4. **Use Premium Service Bus tier**: For production workloads requiring VNet integration or geo-disaster recovery.
5. **Enable diagnostic settings**: Route logs and metrics from all resources to Azure Monitor / Log Analytics.
6. **Store `postgresAdminPassword` in Key Vault**: Use Key Vault references in `parameters.json` instead of passing the password on the command line.
