# Asset Manager - Azure Infrastructure

Bicep Infrastructure as Code (IaC) for deploying the Asset Manager application to Azure.

## Architecture Overview

This template provisions the following Azure resources:

| Resource | Purpose | SKU/Tier |
|----------|---------|----------|
| User-Assigned Managed Identity | Credential-free authentication to all Azure services | N/A |
| Azure Storage Account | Blob storage for original assets and thumbnails | Standard LRS |
| Azure Service Bus Namespace | Message queue for async image processing (web → worker) | Standard |
| Azure Database for PostgreSQL Flexible Server | Managed relational database | Burstable B2ms |

### Resource Layout

```
Resource Group
├── id-assetmgr-{env}               (User-Assigned Managed Identity)
├── st{projectName}{env}{suffix}    (Storage Account)
│   ├── assets                      (Blob Container - original uploads)
│   └── thumbnails                  (Blob Container - generated thumbnails)
├── sb-assetmgr-{env}-{suffix}      (Service Bus Namespace)
│   └── image-processing            (Queue - thumbnail generation requests)
└── psql-assetmgr-{env}-{suffix}    (PostgreSQL Flexible Server)
    └── assets_manager              (Database)
```

### Authentication (Managed Identity)

All services are configured with a **User-Assigned Managed Identity** for secure, credential-free access:

| Service | Role Assigned |
|---------|--------------|
| Azure Blob Storage | `Storage Blob Data Contributor` |
| Azure Service Bus | `Azure Service Bus Data Owner` |
| Azure Database for PostgreSQL | Azure AD Administrator (Service Principal) |

Applications must be configured with the managed identity's **Client ID** as `AZURE_CLIENT_ID`.

---

## Prerequisites

- [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli) installed and logged in
- Azure subscription with permission to create resources and assign RBAC roles
- Contributor + User Access Administrator role on the target resource group (or Owner)

---

## Deployment

### Linux / macOS

```bash
chmod +x infra/deploy.sh
./infra/deploy.sh <resource-group> <location> [environment]

# Example:
./infra/deploy.sh rg-assetmgr-dev eastus dev
```

### Windows (PowerShell)

```powershell
.\infra\deploy.ps1 -ResourceGroup rg-assetmgr-dev -Location eastus -Environment dev
```

### Manual (Azure CLI)

```bash
# Create resource group
az group create --name rg-assetmgr-dev --location eastus

# Deploy
az deployment group create \
  --resource-group rg-assetmgr-dev \
  --template-file infra/main.bicep \
  --parameters infra/parameters.json \
  --parameters postgresAdminPassword="<secure-password>"
```

---

## Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `environmentName` | `dev` | Environment tag used in resource names |
| `location` | resource group location | Azure region for all resources |
| `projectName` | `assetmgr` | Short prefix for resource names (max 8 chars) |
| `postgresAdminLogin` | `pgadmin` | PostgreSQL admin username |
| `postgresAdminPassword` | *(required)* | PostgreSQL admin password (min 8 chars) |
| `postgresDatabaseName` | `assets_manager` | Name of the PostgreSQL database |
| `serviceBusQueueName` | `image-processing` | Service Bus queue name |
| `assetsContainerName` | `assets` | Blob container for uploaded files |
| `thumbnailsContainerName` | `thumbnails` | Blob container for thumbnails |

> **Security Note**: Never commit `postgresAdminPassword` to source control. Pass it at deployment time or use Azure Key Vault references in `parameters.json`.

---

## Outputs

After deployment, the following values are available as outputs:

| Output | Description | Application Config Key |
|--------|-------------|------------------------|
| `managedIdentityClientId` | Managed identity client ID | `AZURE_CLIENT_ID` |
| `storageAccountEndpoint` | Blob storage endpoint URL | `AZURE_STORAGE_ACCOUNT_URL` |
| `assetsContainerName` | Assets container name | `AZURE_STORAGE_ASSETS_CONTAINER` |
| `thumbnailsContainerName` | Thumbnails container name | `AZURE_STORAGE_THUMBNAILS_CONTAINER` |
| `serviceBusHostname` | Service Bus FQDN | `AZURE_SERVICEBUS_NAMESPACE` |
| `serviceBusQueueName` | Queue name | `AZURE_SERVICEBUS_QUEUE_NAME` |
| `postgresFqdn` | PostgreSQL server FQDN | Spring datasource URL host |
| `postgresDatabaseName` | Database name | Spring datasource URL path |

Retrieve outputs with:

```bash
az deployment group show \
  --resource-group rg-assetmgr-dev \
  --name main \
  --query "properties.outputs" \
  --output json
```

---

## Customising for Different Environments

Copy and modify `parameters.json` per environment:

```bash
cp infra/parameters.json infra/parameters.prod.json
# Edit parameters.prod.json for production settings
./infra/deploy.sh rg-assetmgr-prod westeurope prod
```

---

## File Structure

```
infra/
├── main.bicep              # Root template - orchestrates all modules
├── parameters.json         # Default parameter values
├── modules/
│   ├── identity.bicep      # User-assigned managed identity
│   ├── storage.bicep       # Storage account, containers, RBAC
│   ├── servicebus.bicep    # Service Bus namespace, queue, RBAC
│   └── postgresql.bicep    # PostgreSQL flexible server, database, AD admin
├── deploy.sh               # Linux/macOS deployment script
├── deploy.ps1              # Windows PowerShell deployment script
├── README.md               # This file
└── compliance.md           # IaC rules compliance report
```
