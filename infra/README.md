# Azure Infrastructure – Asset Manager Kit

This directory contains Bicep Infrastructure as Code (IaC) templates to provision all Azure resources required for the **Asset Manager Kit** application.

---

## Architecture Overview

| Resource | Purpose | Authentication |
|---|---|---|
| User-Assigned Managed Identity | Single identity used by the application to authenticate with all Azure services | — |
| Azure Blob Storage | Replaces AWS S3 for file uploads and asset storage | Managed Identity (Storage Blob Data Contributor) |
| Azure Service Bus (Standard) | Replaces RabbitMQ for thumbnail generation messaging between web and worker modules | Managed Identity (Azure Service Bus Data Owner) |
| Azure Database for PostgreSQL Flexible Server | Managed PostgreSQL with Azure AD authentication | Managed Identity (Entra AD admin) |

---

## Directory Structure

```
infra/
├── main.bicep          # Root template – orchestrates all modules
├── parameters.json     # Environment-specific parameter values
├── modules/
│   ├── identity.bicep  # User-Assigned Managed Identity
│   ├── storage.bicep   # Azure Blob Storage account + container + RBAC
│   ├── servicebus.bicep# Azure Service Bus namespace + queue + RBAC
│   └── postgresql.bicep# Azure Database for PostgreSQL flexible server
├── deploy.sh           # Bash deployment script (Linux / macOS)
├── deploy.ps1          # PowerShell deployment script (Windows)
└── README.md           # This file
```

---

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Azure CLI | ≥ 2.50 | [docs.microsoft.com/cli/azure/install-azure-cli](https://docs.microsoft.com/cli/azure/install-azure-cli) |
| Bicep CLI | ≥ 0.25 | `az bicep install` |

Authenticate before deploying:

```bash
az login
az bicep install
```

---

## Parameters

| Parameter | Required | Default | Description |
|---|---|---|---|
| `location` | No | `eastus` | Azure region for all resources |
| `environment` | No | `dev` | Environment suffix (`dev`, `staging`, `prod`) |
| `resourceSuffix` | No | auto | Unique suffix for globally-scoped names |
| `assetsContainerName` | No | `assets` | Blob container name |
| `thumbnailQueueName` | No | `thumbnail-requests` | Service Bus queue name |
| `databaseName` | No | `assetsdb` | PostgreSQL database name |
| `postgresAdminLogin` | No | `pgadmin` | PostgreSQL admin username |
| `postgresAdminPassword` | **Yes** | — | PostgreSQL admin password (provide at deploy time) |

---

## Deploy (Linux / macOS)

```bash
chmod +x infra/deploy.sh

# Deploy to a new or existing resource group
./infra/deploy.sh \
  -g assetmgr-dev-rg \
  -s <your-subscription-id> \
  -l eastus \
  -e dev
```

You will be prompted for the PostgreSQL password. Alternatively, pass it with `-p <password>`.

---

## Deploy (Windows PowerShell)

```powershell
.\infra\deploy.ps1 `
  -ResourceGroup "assetmgr-dev-rg" `
  -SubscriptionId "<your-subscription-id>" `
  -Location "eastus" `
  -Environment "dev"
```

---

## Manual Deployment (Azure CLI)

```bash
RESOURCE_GROUP="assetmgr-dev-rg"
SUBSCRIPTION_ID="<your-subscription-id>"

az account set --subscription $SUBSCRIPTION_ID

az group create --name $RESOURCE_GROUP --location eastus

az deployment group create \
  --name assetmgr-infra \
  --resource-group $RESOURCE_GROUP \
  --template-file infra/main.bicep \
  --parameters infra/parameters.json \
  --parameters postgresAdminPassword='<your-secure-password>'
```

---

## Outputs

After a successful deployment the following values are emitted:

| Output | Description |
|---|---|
| `managedIdentityId` | Resource ID of the managed identity |
| `managedIdentityClientId` | Client ID – set as `AZURE_CLIENT_ID` in application config |
| `blobEndpoint` | Primary blob service endpoint URL |
| `assetsContainerName` | Name of the blob container |
| `serviceBusHostname` | Service Bus namespace hostname |
| `thumbnailQueueName` | Name of the thumbnail queue |
| `postgresServerFqdn` | PostgreSQL server FQDN |
| `postgresDatabaseName` | Application database name |
| `postgresJdbcConnectionString` | Full JDBC URL with Azure AD passwordless plugin |

---

## Application Configuration

After deployment, configure each Spring Boot module with the following environment variables (replace `<value>` with actual output values):

```yaml
# application.yaml (or environment variables)
spring:
  datasource:
    url: <postgresJdbcConnectionString>
  cloud:
    azure:
      credential:
        managed-identity-enabled: true
        client-id: <managedIdentityClientId>
      servicebus:
        namespace: <serviceBusHostname>
      storage:
        blob:
          endpoint: <blobEndpoint>
          container-name: <assetsContainerName>
```

---

## Security Notes

- Public blob access is **disabled** on the storage account.
- TLS 1.2 is enforced on all services.
- The PostgreSQL server allows access from Azure services only (firewall rule `0.0.0.0`–`0.0.0.0`). For production, restrict to your VNet or specific IP ranges.
- Passwords are never stored in the templates – use Key Vault references or pass them at deploy time.
