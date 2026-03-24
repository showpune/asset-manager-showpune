# Asset Manager – Azure Infrastructure

This directory contains Bicep IaC templates that provision all Azure resources required to run the **Asset Manager** application on Azure. These templates replace the existing AWS S3 / RabbitMQ dependencies with equivalent Azure-native managed services.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Asset Manager                                                  │
│                                                                 │
│  ┌─────────────┐   upload/list/delete   ┌───────────────────┐  │
│  │  Web Module │ ─────────────────────► │  Azure Blob       │  │
│  │  (port 8080)│                        │  Storage          │  │
│  │             │   image-processing msg ├───────────────────┤  │
│  │             │ ─────────────────────► │  Azure Service    │  │
│  └──────┬──────┘                        │  Bus (queue)      │  │
│         │                               └────────┬──────────┘  │
│         │ JPA / JDBC                             │ listen       │
│         ▼                                        ▼             │
│  ┌─────────────────────────────┐  ┌─────────────────────────┐  │
│  │  Azure Database for         │  │  Worker Module          │  │
│  │  PostgreSQL Flexible Server │◄─│  (port 8081)            │  │
│  └─────────────────────────────┘  └─────────────────────────┘  │
│                                                                 │
│  All services authenticate via User-Assigned Managed Identity   │
└─────────────────────────────────────────────────────────────────┘
```

## Resources Provisioned

| Resource | Azure Service | Replaces |
|----------|--------------|---------|
| User-Assigned Managed Identity | `Microsoft.ManagedIdentity/userAssignedIdentities` | AWS IAM credentials |
| Storage Account + Container | `Microsoft.Storage/storageAccounts` | AWS S3 |
| Service Bus Namespace + Queue | `Microsoft.ServiceBus/namespaces` | RabbitMQ |
| PostgreSQL Flexible Server + DB | `Microsoft.DBforPostgreSQL/flexibleServers` | Local PostgreSQL |

## File Structure

```
infra/
├── main.bicep              # Orchestrator – calls all modules
├── parameters.json         # Default parameter values (dev environment)
├── modules/
│   ├── identity.bicep      # User-assigned managed identity
│   ├── storage.bicep       # Azure Blob Storage + RBAC
│   ├── servicebus.bicep    # Azure Service Bus namespace + queue + RBAC
│   └── postgresql.bicep    # PostgreSQL flexible server + database
├── deploy.sh               # Deployment script (Linux / macOS)
├── deploy.ps1              # Deployment script (Windows)
├── README.md               # This file
└── compliance.md           # Rules compliance report
```

## Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `location` | resource group location | Azure region for all resources |
| `environmentName` | `dev` | Environment tag (`dev`, `test`, `prod`) |
| `projectName` | `assetmgr` | Short prefix used in resource names (max 10 chars) |
| `postgresAdminLogin` | `pgadmin` | PostgreSQL administrator username |
| `postgresAdminPassword` | *(required)* | PostgreSQL administrator password – never stored in parameters.json |
| `storageSku` | `Standard_LRS` | Storage account replication SKU |
| `serviceBusSku` | `Standard` | Service Bus tier (`Standard` or `Premium`) |
| `postgresSkuName` | `Standard_B2s` | PostgreSQL compute SKU |
| `postgresSkuTier` | `Burstable` | PostgreSQL compute tier |
| `postgresStorageSizeGB` | `32` | PostgreSQL storage size (32 GB) |

## Outputs

After a successful deployment the following values are printed and available as ARM deployment outputs:

| Output | Description |
|--------|-------------|
| `managedIdentityId` | Resource ID of the managed identity |
| `managedIdentityClientId` | Client ID – set as `AZURE_CLIENT_ID` in application config |
| `storageAccountName` | Storage account name |
| `storageBlobEndpoint` | Blob service endpoint URL |
| `storageContainerName` | Blob container name (`assets`) |
| `serviceBusNamespaceName` | Service Bus namespace name |
| `serviceBusHostname` | Namespace hostname (e.g., `assetmgr-dev-sb.servicebus.windows.net`) |
| `serviceBusQueueName` | Queue name (`image-processing`) |
| `postgresServerName` | PostgreSQL server name |
| `postgresFqdn` | PostgreSQL fully qualified domain name |
| `postgresDatabaseName` | Database name (`assets_manager`) |

## Prerequisites

- [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli) ≥ 2.50
- An active Azure subscription
- Resource provider registrations: `Microsoft.Storage`, `Microsoft.ServiceBus`, `Microsoft.DBforPostgreSQL`, `Microsoft.ManagedIdentity`

## Deployment

### Linux / macOS

```bash
# Log in to Azure
az login

# Set required variables
export RESOURCE_GROUP="asset-manager-rg"
export LOCATION="eastus"
export ENVIRONMENT_NAME="dev"
export POSTGRES_ADMIN_PASSWORD="<your-password>"

# Make the script executable and run
chmod +x infra/deploy.sh
./infra/deploy.sh
```

Or pass arguments directly:

```bash
POSTGRES_ADMIN_PASSWORD="<your-password>" \
  ./infra/deploy.sh --resource-group asset-manager-rg --location eastus --env dev
```

### Windows (PowerShell)

```powershell
# Log in to Azure
az login

# Deploy
$env:POSTGRES_ADMIN_PASSWORD = "<your-password>"
.\infra\deploy.ps1 -ResourceGroup "asset-manager-rg" -Location "eastus" -Environment "dev"
```

### Manual Azure CLI

```bash
az group create --name asset-manager-rg --location eastus

az deployment group create \
  --resource-group asset-manager-rg \
  --template-file infra/main.bicep \
  --parameters infra/parameters.json \
  --parameters postgresAdminPassword="<your-password>"
```

## Application Configuration

After deployment, update both `web` and `worker` `application.properties` with the deployment outputs:

### web/src/main/resources/application.properties

```properties
# Azure Blob Storage (replaces AWS S3)
azure.storage.account-name=<storageAccountName>
azure.storage.blob-endpoint=<storageBlobEndpoint>
azure.storage.container-name=assets

# Azure Service Bus (replaces RabbitMQ)
spring.jms.servicebus.namespace=<serviceBusHostname>
spring.jms.servicebus.pricing-tier=premium
spring.jms.servicebus.passwordless-enabled=true
spring.cloud.azure.credential.managed-identity-enabled=true
spring.cloud.azure.credential.client-id=<managedIdentityClientId>

# PostgreSQL
spring.datasource.url=jdbc:postgresql://<postgresFqdn>:5432/assets_manager
spring.datasource.username=pgadmin
spring.datasource.password=<password>
```

### worker/src/main/resources/application.properties

```properties
# Same Azure Blob Storage, Service Bus, and PostgreSQL settings as above
azure.client-id=<managedIdentityClientId>
```

## Managed Identity RBAC Assignments

The templates automatically create the following role assignments for the managed identity:

| Role | Scope | Purpose |
|------|-------|---------|
| Storage Blob Data Contributor | Storage Account | Read/write blobs in the assets container |
| Azure Service Bus Data Owner | Service Bus Namespace | Send and receive messages on the image-processing queue |

The managed identity is also attached to the PostgreSQL server for future Azure AD authentication support (`activeDirectoryAuth: Enabled`).

## Customization

To deploy to a different environment, override parameters at deploy time:

```bash
az deployment group create \
  --resource-group asset-manager-prod-rg \
  --template-file infra/main.bicep \
  --parameters infra/parameters.json \
  --parameters \
    environmentName=prod \
    postgresSkuName=Standard_D2s_v3 \
    postgresSkuTier=GeneralPurpose \
    serviceBusSku=Standard \
    storageSku=Standard_GRS \
    postgresAdminPassword="<prod-password>"
```
