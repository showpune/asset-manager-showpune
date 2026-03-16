# assets-manager — Azure Infrastructure

This directory contains Bicep templates to provision all Azure resources required
by the **assets-manager** application (web + worker modules).

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                     Azure Resource Group                         │
│                                                                  │
│  ┌─────────────┐    ┌──────────────────┐    ┌────────────────┐  │
│  │   Web App   │    │   Worker App     │    │   Managed      │  │
│  │  (Port 8080)│    │  (Port 8081)     │    │   Identity     │  │
│  └──────┬──────┘    └────────┬─────────┘    └───────┬────────┘  │
│         │                   │                       │           │
│         └────────┬──────────┘                       │           │
│                  │ (uses Managed Identity)           │           │
│    ┌─────────────▼──────────────────────────────────▼────────┐  │
│    │                   RBAC Role Assignments                  │  │
│    │  • Storage Blob Data Contributor  (storage account)      │  │
│    │  • Azure Service Bus Data Owner   (sb namespace)         │  │
│    │  • AAD Admin                      (postgresql server)    │  │
│    └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  Azure Blob      │  │ Azure Service Bus │  │ PostgreSQL   │  │
│  │  Storage         │  │ Namespace         │  │ Flexible     │  │
│  │  (assets-manager │  │  Queue:           │  │ Server       │  │
│  │   container)     │  │  image-processing │  │ DB: assets   │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

## Resources Provisioned

| Resource | Azure Service | Purpose |
|---|---|---|
| `id-assets-manager-<env>` | User-Assigned Managed Identity | Credential-free auth for all services |
| `stassetsma<env><suffix>` | Azure Storage Account | Asset file storage (replaces AWS S3) |
| `assets` container | Azure Blob Container | Stores uploaded images and thumbnails |
| `sb-assets-manager-<env>-<suffix>` | Azure Service Bus Namespace | Message broker (replaces RabbitMQ) |
| `image-processing` queue | Service Bus Queue | Async image processing pipeline |
| `psql-assets-manager-<env>-<suffix>` | PostgreSQL Flexible Server | Application database |
| `assets_manager` | PostgreSQL Database | Application schema and data |

## File Structure

```
infra/
├── main.bicep           # Orchestrates all modules; defines shared parameters
├── parameters.json      # Environment-specific parameter values
├── modules/
│   ├── managedidentity.bicep  # User-Assigned Managed Identity
│   ├── storage.bicep          # Azure Blob Storage + RBAC assignment
│   ├── servicebus.bicep       # Service Bus namespace + queue + RBAC
│   └── postgresql.bicep       # PostgreSQL Flexible Server + AAD admin
├── deploy.sh            # Deployment script (Linux/macOS)
├── deploy.ps1           # Deployment script (Windows)
├── README.md            # This file
└── compliance.md        # IaC rules compliance report
```

## Prerequisites

- Azure CLI ≥ 2.50.0  (`az --version`)
- Logged in to Azure  (`az login`)
- Bicep CLI           (`az bicep install`)
- Contributor role on the target subscription or resource group

## Deployment

### Linux / macOS

```bash
chmod +x ./infra/deploy.sh

./infra/deploy.sh \
  --resource-group "rg-assets-manager-dev" \
  --location "eastus" \
  --environment "dev" \
  --postgres-password "YourStr0ngP@ssword!"
```

Preview changes without applying:

```bash
./infra/deploy.sh \
  --resource-group "rg-assets-manager-dev" \
  --what-if
```

### Windows (PowerShell)

```powershell
.\infra\deploy.ps1 `
  -ResourceGroup "rg-assets-manager-dev" `
  -Location "eastus" `
  -Environment "dev" `
  -PostgresPassword (ConvertTo-SecureString "YourStr0ngP@ssword!" -AsPlainText -Force)
```

Preview changes:

```powershell
.\infra\deploy.ps1 -ResourceGroup "rg-assets-manager-dev" -WhatIf
```

### Manual (Azure CLI)

```bash
az deployment group create \
  --resource-group "rg-assets-manager-dev" \
  --template-file infra/main.bicep \
  --parameters infra/parameters.json \
  --parameters postgresAdminPassword="YourStr0ngP@ssword!"
```

## Parameters

| Parameter | Default | Description |
|---|---|---|
| `location` | `eastus` | Azure region |
| `appName` | `assets-manager` | Name prefix for all resources |
| `environment` | `dev` | Deployment environment (`dev`, `staging`, `prod`) |
| `storageSkuName` | `Standard_LRS` | Storage account redundancy SKU |
| `serviceBusTier` | `Standard` | Service Bus pricing tier |
| `postgresSkuName` | `Standard_D2ds_v4` | PostgreSQL compute SKU |
| `postgresSkuTier` | `GeneralPurpose` | PostgreSQL compute tier |
| `postgresStorageSizeGB` | `32` | PostgreSQL storage size in GB |
| `postgresVersion` | `16` | PostgreSQL major version |
| `postgresAdminLogin` | `pgadmin` | PostgreSQL admin username |
| `postgresAdminPassword` | _(required)_ | PostgreSQL admin password (secure) |
| `postgresBackupRetentionDays` | `7` | Backup retention period |

## Outputs

After deployment, the following values are emitted and can be used to configure
the Spring Boot applications:

| Output | Description |
|---|---|
| `managedIdentityClientId` | Set as `AZURE_CLIENT_ID` in both web and worker |
| `managedIdentityId` | Managed Identity resource ID for App Service/Container config |
| `storageAccountName` | Azure Storage account name |
| `storageContainerName` | Blob container name (default: `assets`) |
| `storageBlobEndpoint` | Blob service endpoint URL |
| `serviceBusNamespace` | Service Bus namespace name |
| `serviceBusEndpoint` | Namespace hostname for connection (e.g. `sb-...-dev-abc123.servicebus.windows.net`) |
| `serviceBusQueueName` | Queue name (`image-processing`) |
| `postgresHostname` | PostgreSQL server FQDN |
| `postgresPort` | PostgreSQL port (5432) |
| `postgresDatabaseName` | Database name (`assets_manager`) |
| `postgresJdbcUrl` | JDBC URL (password-based, for migration phase) |
| `postgresJdbcUrlMI` | JDBC URL using Managed Identity (passwordless, recommended) |

## Application Configuration

After provisioning, update the Spring Boot `application.properties` as follows.

### web module

```properties
# Azure Blob Storage (replaces AWS S3)
azure.storage.account-name=${storageAccountName}
azure.storage.container-name=${storageContainerName}

# Azure Service Bus (replaces RabbitMQ)
spring.jms.servicebus.namespace=${serviceBusNamespace}
spring.jms.servicebus.pricing-tier=standard

# PostgreSQL
spring.datasource.url=${postgresJdbcUrl}
spring.datasource.username=pgadmin
spring.datasource.password=<admin-password>

# Managed Identity client ID (used by DefaultAzureCredential)
AZURE_CLIENT_ID=${managedIdentityClientId}
```

### worker module

```properties
# Same Azure service configuration as web module
# Queue name remains 'image-processing'
server.port=8081
```

## Security Notes

- The Managed Identity is granted **Storage Blob Data Contributor** on the
  storage account and **Service Bus Data Owner** on the namespace.
  For production, split into separate identities with **Data Sender** (web)
  and **Data Receiver** (worker) roles for least-privilege access.
- PostgreSQL password authentication is enabled alongside AAD authentication
  to support the migration phase. Disable password auth once all applications
  use `DefaultAzureCredential`.
- Public network access is enabled for PostgreSQL with an Azure Services
  firewall rule. Restrict to specific IP ranges for production workloads or
  use VNet integration.
