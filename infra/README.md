# Azure Infrastructure — asset-manager-showpune

Bicep IaC templates to provision all Azure resources required by the asset-manager-showpune application.

## Architecture

```
asset-manager-showpune (web + worker modules)
 ├── Azure Storage Account  (Blob: assets container)
 ├── Azure Service Bus Namespace  (Queue: image-processing)
 ├── Azure Database for PostgreSQL Flexible Server  (DB: asset_manager)
 └── User-Assigned Managed Identity  (passwordless access to all services)
```

## Resources Provisioned

| Resource Type | Name Pattern | SKU | Purpose |
|---|---|---|---|
| User-Assigned Managed Identity | `azmi<token>` | N/A | Passwordless auth for Storage, Service Bus, and PostgreSQL |
| Storage Account | `azst<token>` | Standard LRS | Blob storage for asset images and thumbnails |
| Blob Container | `assets` | N/A | Container for uploaded assets |
| Service Bus Namespace | `azsb<token>` | Standard | Async messaging between web and worker |
| Service Bus Queue | `image-processing` | N/A | Image processing message queue |
| PostgreSQL Flexible Server | `azpg<token>` | Burstable B1ms | Metadata storage for web and worker |
| PostgreSQL Database | `asset_manager` | N/A | Application database |

> Resource names use `az{prefix}{uniqueToken}` format where `uniqueToken = uniqueString(subscriptionId, resourceGroupId, location, environmentName)`.

## Prerequisites

- [Azure CLI](https://aka.ms/installazurecli) installed and authenticated (`az login`)
- Contributor role on the target subscription/resource group

## Parameters

| Parameter | Default | Description |
|---|---|---|
| `environmentName` | `dev` | Environment label (used in resource token) |
| `location` | `eastus` | Azure region for all resources |
| `administratorLogin` | `pgadmin` | PostgreSQL admin username (server management only) |
| `administratorLoginPassword` | *(required)* | PostgreSQL admin password — **never commit to source control** |

## Deployment

### Windows (PowerShell)

```powershell
cd infra

$password = ConvertTo-SecureString "YourSecureP@ssword1" -AsPlainText -Force

.\deploy.ps1 `
  -ResourceGroupName "rg-asset-manager-dev" `
  -Location "eastus" `
  -EnvironmentName "dev" `
  -AdministratorLoginPassword $password
```

### Linux / macOS (Bash)

```bash
cd infra
chmod +x deploy.sh

./deploy.sh \
  -g rg-asset-manager-dev \
  -l eastus \
  -e dev \
  -p "YourSecureP@ssword1"
```

### Manual (Azure CLI)

```bash
az group create --name rg-asset-manager-dev --location eastus

az deployment group create \
  --resource-group rg-asset-manager-dev \
  --template-file main.bicep \
  --parameters main.parameters.json \
  --parameters administratorLoginPassword="YourSecureP@ssword1"
```

## Post-Deployment Configuration

### 1. Update Application Properties

After deployment, update `application.properties` in **both** `web` and `worker` modules:

```properties
# Azure Storage
azure.storage.account-name=<storageAccountName output>

# Azure Service Bus
spring.cloud.azure.servicebus.namespace=<serviceBusNamespace output>

# Azure PostgreSQL
spring.datasource.url=jdbc:postgresql://<postgresFqdn output>:5432/asset_manager
```

### 2. Assign Managed Identity to Compute Service

Assign the provisioned managed identity (`managedIdentityClientId` output) to your App Service or Container App.

### 3. Service Connector (Azure Container Apps only)

When deploying to Azure Container Apps, run Service Connector to configure Managed Identity access to PostgreSQL:

```bash
az extension add --name serviceconnector-passwordless

az containerapp connection create postgres-flexible \
  --connection asset-db-conn \
  --user-identity client-id=<managedIdentityClientId> subs-id=<subscriptionId> \
  --source-id <containerapp-resource-id> \
  --tg <resource-group> \
  --server <postgresServerName> \
  --database asset_manager \
  --client-type springBoot -y
```

## File Structure

```
infra/
├── main.bicep              # Main template — orchestrates all modules
├── main.parameters.json    # Default parameter values
├── modules/
│   ├── identity.bicep      # User-Assigned Managed Identity
│   ├── storage.bicep       # Storage Account + Blob Container + RBAC
│   ├── servicebus.bicep    # Service Bus Namespace + Queue + RBAC
│   └── postgresql.bicep    # PostgreSQL Flex Server + DB + Firewall + Entra Admin
├── deploy.ps1              # Deployment script (Windows/PowerShell)
├── deploy.sh               # Deployment script (Linux/macOS)
├── README.md               # This file
└── compliance.md           # IaC rules compliance report
```
