# Asset Manager – Azure Infrastructure

This directory contains Bicep IaC templates to provision all Azure infrastructure required by the **Asset Manager** application.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  Resource Group                                                     │
│                                                                     │
│  ┌───────────────┐   publishes    ┌───────────────────────────────┐ │
│  │  Web App      │ ─────────────► │  Azure Service Bus            │ │
│  │  (web module) │               │  Namespace                    │ │
│  │               │ ◄─────────────│  Queue: image-processing      │ │
│  └───────┬───────┘   subscribes  └───────────────────────────────┘ │
│          │                                      ▲                  │
│          │ reads/writes                         │ subscribes       │
│          ▼                                      │                  │
│  ┌───────────────┐              ┌───────────────┴───────────────┐  │
│  │  Azure Blob   │              │  Worker App                   │  │
│  │  Storage      │◄─────────────│  (worker module)              │  │
│  │  (assets)     │ reads/writes └───────────────────────────────┘  │
│  └───────────────┘                                                 │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Azure Database for PostgreSQL Flexible Server              │   │
│  │  Database: assets_manager                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────────────────────┐  ┌──────────────────────────────┐    │
│  │  Web App Managed Identity│  │ Worker Managed Identity       │    │
│  └──────────────────────────┘  └──────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

## Resources Provisioned

| Resource | Type | Description |
|---|---|---|
| `assets<suffix><env>` | Azure Storage Account | Blob storage for asset files |
| `assets` container | Blob Container | Container for uploaded assets |
| `asset-manager-sb-<env>-<suffix>` | Service Bus Namespace | Message broker replacing RabbitMQ |
| `image-processing` | Service Bus Queue | Queue for image processing messages |
| `asset-manager-pg-<env>-<suffix>` | PostgreSQL Flexible Server | Application database |
| `assets_manager` | PostgreSQL Database | Application schema |
| `asset-manager-web-identity-<env>` | User-Assigned Managed Identity | Identity for web module |
| `asset-manager-worker-identity-<env>` | User-Assigned Managed Identity | Identity for worker module |

## Managed Identity Role Assignments

| Identity | Resource | Role |
|---|---|---|
| Web App Identity | Storage Account | Storage Blob Data Contributor |
| Worker Identity | Storage Account | Storage Blob Data Contributor |
| Web App Identity | Service Bus Namespace | Azure Service Bus Data Sender |
| Web App Identity | Service Bus Namespace | Azure Service Bus Data Receiver |
| Worker Identity | Service Bus Namespace | Azure Service Bus Data Receiver |
| Web App Identity | PostgreSQL Server | Microsoft Entra Admin |

## File Structure

```
infra/
├── main.bicep              # Main orchestration template
├── parameters.json         # Default parameters (dev environment)
├── deploy.sh               # Deployment script (Linux/macOS)
├── deploy.ps1              # Deployment script (Windows)
├── README.md               # This file
├── compliance.md           # Rules compliance report
└── modules/
    ├── managed-identity.bicep  # User-assigned managed identities
    ├── storage.bicep           # Azure Blob Storage account
    ├── servicebus.bicep        # Azure Service Bus namespace + queues
    └── postgresql.bicep        # Azure Database for PostgreSQL
```

## Prerequisites

- [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli) ≥ 2.50
- [Bicep CLI](https://learn.microsoft.com/en-us/azure/azure-resource-manager/bicep/install) ≥ 0.24 (`az bicep install`)
- An Azure subscription with Contributor permissions on the target resource group

## Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `location` | string | `eastus` | Azure region for all resources |
| `environmentName` | string | `dev` | Environment tag (`dev`, `staging`, `prod`) |
| `appName` | string | `asset-manager` | Base name for resource naming |
| `postgresAdminLogin` | string | `pgadmin` | PostgreSQL administrator username |
| `postgresAdminPassword` | securestring | *(required)* | PostgreSQL administrator password |
| `postgresSkuName` | string | `Standard_D2s_v3` | PostgreSQL compute SKU |
| `postgresSkuTier` | string | `GeneralPurpose` | PostgreSQL pricing tier |
| `postgresStorageSizeGB` | int | `32` | PostgreSQL storage size (GB) |
| `postgresVersion` | string | `16` | PostgreSQL major version |
| `storageSku` | string | `Standard_LRS` | Storage account replication SKU |
| `serviceBusSku` | string | `Standard` | Service Bus tier |
| `tags` | object | *see below* | Tags applied to all resources |

## Deployment

### Linux / macOS

```bash
cd infra

# Make the script executable (first time only)
chmod +x deploy.sh

# Deploy to dev
./deploy.sh \
  --resource-group myapp-dev-rg \
  --location eastus \
  --environment dev \
  --postgres-password "YourSecurePassword123!"

# What-if analysis (no changes applied)
./deploy.sh \
  --resource-group myapp-dev-rg \
  --what-if \
  --postgres-password "dummy"
```

### Windows (PowerShell)

```powershell
cd infra

# Deploy to dev
.\deploy.ps1 `
  -ResourceGroup "myapp-dev-rg" `
  -Location "eastus" `
  -Environment "dev" `
  -PostgresPassword "YourSecurePassword123!"

# What-if analysis
.\deploy.ps1 -ResourceGroup "myapp-dev-rg" -WhatIf
```

### Direct Azure CLI

```bash
az deployment group create \
  --resource-group myapp-dev-rg \
  --template-file infra/main.bicep \
  --parameters infra/parameters.json \
  --parameters postgresAdminPassword="YourSecurePassword123!"
```

## Outputs

After deployment, the following outputs are available:

| Output | Description |
|---|---|
| `webAppClientId` | Managed Identity client ID for the web app (`AZURE_CLIENT_ID`) |
| `workerClientId` | Managed Identity client ID for the worker (`AZURE_CLIENT_ID`) |
| `storageAccountName` | Storage account name |
| `blobEndpoint` | Storage account blob endpoint URL |
| `containerName` | Blob container name for assets |
| `serviceBusNamespaceName` | Service Bus namespace name |
| `serviceBusEndpoint` | Service Bus endpoint URL |
| `imageProcessingQueueName` | Name of the image processing queue |
| `postgresServerFqdn` | PostgreSQL server hostname |
| `postgresDatabaseName` | Database name |
| `postgresJdbcUrl` | JDBC connection string (passwordless, for use with Managed Identity) |

## Application Configuration

After deployment, configure the application with the following environment variables (using Managed Identity — no secrets required):

### Web Module (`web/src/main/resources/application.properties`)

```properties
# Azure Blob Storage (replaces AWS S3)
azure.storage.account-name=<storageAccountName output>
azure.storage.container-name=assets
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=<webAppClientId output>

# Azure Service Bus (replaces RabbitMQ)
spring.jms.servicebus.namespace=<serviceBusNamespaceName output>
spring.jms.servicebus.pricing-tier=standard
spring.cloud.azure.servicebus.namespace=<serviceBusNamespaceName output>

# PostgreSQL (passwordless with Managed Identity)
spring.datasource.url=<postgresJdbcUrl output>
spring.datasource.username=<webAppIdentityName>@<postgresServerName>
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Worker Module (`worker/src/main/resources/application.properties`)

```properties
# Azure Blob Storage (replaces AWS S3)
azure.storage.account-name=<storageAccountName output>
azure.storage.container-name=assets
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=<workerClientId output>

# Azure Service Bus (replaces RabbitMQ)
spring.cloud.azure.servicebus.namespace=<serviceBusNamespaceName output>

# PostgreSQL (passwordless with Managed Identity)
spring.datasource.url=<postgresJdbcUrl output>
```

## Security Notes

- Storage account uses **Managed Identity** authentication (`allowSharedKeyAccess: false`)
- Service Bus uses **Managed Identity** authentication (`disableLocalAuth: true`)
- PostgreSQL uses **Microsoft Entra authentication** alongside password auth
- No secrets are stored in Bicep files or parameters; the password is referenced from Key Vault
- All storage traffic is HTTPS only (`supportsHttpsTrafficOnly: true`, `minimumTlsVersion: TLS1_2`)
- Blob containers have public access disabled
