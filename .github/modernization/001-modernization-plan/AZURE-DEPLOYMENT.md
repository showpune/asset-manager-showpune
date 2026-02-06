# Azure Deployment Guide

## Overview
This guide walks through the steps to deploy the Asset Manager application to Azure using Azure Blob Storage and managed identity authentication.

## Prerequisites
- Azure CLI installed and configured
- Maven installed
- Java 17 JDK installed
- Access to an Azure subscription

## Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                        Azure Cloud                          │
│                                                             │
│  ┌──────────────┐         ┌──────────────────┐            │
│  │ Azure App    │         │ Azure Blob       │            │
│  │ Service      │────────>│ Storage          │            │
│  │ (Web)        │         │                  │            │
│  └──────────────┘         └──────────────────┘            │
│         │                                                   │
│         │                                                   │
│         v                                                   │
│  ┌──────────────┐         ┌──────────────────┐            │
│  │ RabbitMQ     │────────>│ Azure Container  │            │
│  │ (VM/ACI)     │         │ Instance         │            │
│  │              │         │ (Worker)         │            │
│  └──────────────┘         └──────────────────┘            │
│         │                          │                       │
│         │                          │                       │
│         v                          v                       │
│  ┌─────────────────────────────────────┐                  │
│  │ Azure Database for PostgreSQL       │                  │
│  │                                     │                  │
│  └─────────────────────────────────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Step 1: Setup Azure Resources

### Create Resource Group
```bash
RESOURCE_GROUP=asset-manager-rg
LOCATION=eastus

az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### Create Azure Storage Account
```bash
STORAGE_ACCOUNT=assetmgrstorage$(date +%s)  # Add timestamp for uniqueness
CONTAINER_NAME=assets

# Create storage account
az storage account create \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_LRS \
  --kind StorageV2 \
  --access-tier Hot

# Create container for assets
az storage container create \
  --name $CONTAINER_NAME \
  --account-name $STORAGE_ACCOUNT \
  --auth-mode login
```

### Create PostgreSQL Database
```bash
DB_SERVER=asset-manager-db
DB_NAME=assets_manager
DB_ADMIN=adminuser
DB_PASSWORD=YourSecurePassword123!

az postgres flexible-server create \
  --name $DB_SERVER \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --admin-user $DB_ADMIN \
  --admin-password $DB_PASSWORD \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 14 \
  --storage-size 32

# Create database
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $DB_SERVER \
  --database-name $DB_NAME

# Configure firewall to allow Azure services
az postgres flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name $DB_SERVER \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

### Create RabbitMQ (VM or Container Instance)
For simplicity, deploy RabbitMQ using Azure Container Instances:

```bash
az container create \
  --resource-group $RESOURCE_GROUP \
  --name rabbitmq \
  --image rabbitmq:3-management \
  --dns-name-label asset-manager-rabbitmq-${LOCATION} \
  --ports 5672 15672 \
  --cpu 1 \
  --memory 1 \
  --environment-variables \
    RABBITMQ_DEFAULT_USER=admin \
    RABBITMQ_DEFAULT_PASS=SecurePassword123!

# Get RabbitMQ hostname
RABBITMQ_HOST=$(az container show --resource-group $RESOURCE_GROUP --name rabbitmq --query ipAddress.fqdn -o tsv)
```

## Step 2: Deploy Web Application

### Create App Service
```bash
APP_NAME=asset-manager-web-$(date +%s)

# Create App Service Plan (Linux)
az appservice plan create \
  --name asset-manager-plan \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --is-linux \
  --sku B2

# Create Web App
az webapp create \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --plan asset-manager-plan \
  --runtime "JAVA:17-java17"
```

### Enable Managed Identity
```bash
# Enable system-assigned managed identity
az webapp identity assign \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP

# Get the principal ID
PRINCIPAL_ID=$(az webapp identity show \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query principalId \
  --output tsv)

echo "Web App Principal ID: $PRINCIPAL_ID"
```

### Grant Storage Access
```bash
# Get storage account resource ID
STORAGE_ID=$(az storage account show \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query id \
  --output tsv)

# Assign Storage Blob Data Contributor role
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID

echo "Storage access granted to web app"
```

### Configure Application Settings
```bash
az webapp config appsettings set \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT \
    AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME \
    SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_SERVER}.postgres.database.azure.com:5432/${DB_NAME}?sslmode=require" \
    SPRING_DATASOURCE_USERNAME=$DB_ADMIN \
    SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
    SPRING_RABBITMQ_HOST=$RABBITMQ_HOST \
    SPRING_RABBITMQ_PORT=5672 \
    SPRING_RABBITMQ_USERNAME=admin \
    SPRING_RABBITMQ_PASSWORD=SecurePassword123!
```

### Build and Deploy
```bash
# Navigate to project root
cd /path/to/asset-manager

# Build the application
mvn clean package -DskipTests

# Deploy to Azure
az webapp deploy \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --src-path web/target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar

echo "Web application deployed!"
echo "URL: https://${APP_NAME}.azurewebsites.net"
```

## Step 3: Deploy Worker Service

### Option A: Azure Container Instance
```bash
WORKER_NAME=asset-manager-worker

# Build worker JAR
cd worker
mvn clean package -DskipTests
cd ..

# Create container image (requires Docker and ACR)
# Or use Azure Container Instance with Java runtime
```

### Option B: Azure App Service
```bash
WORKER_APP_NAME=asset-manager-worker-$(date +%s)

# Create Web App for worker
az webapp create \
  --name $WORKER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --plan asset-manager-plan \
  --runtime "JAVA:17-java17"

# Enable managed identity
az webapp identity assign \
  --name $WORKER_APP_NAME \
  --resource-group $RESOURCE_GROUP

# Get principal ID
WORKER_PRINCIPAL_ID=$(az webapp identity show \
  --name $WORKER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query principalId \
  --output tsv)

# Grant storage access
az role assignment create \
  --assignee $WORKER_PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID

# Configure settings
az webapp config appsettings set \
  --name $WORKER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT \
    AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME \
    SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_SERVER}.postgres.database.azure.com:5432/${DB_NAME}?sslmode=require" \
    SPRING_DATASOURCE_USERNAME=$DB_ADMIN \
    SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
    SPRING_RABBITMQ_HOST=$RABBITMQ_HOST \
    SPRING_RABBITMQ_PORT=5672 \
    SPRING_RABBITMQ_USERNAME=admin \
    SPRING_RABBITMQ_PASSWORD=SecurePassword123!

# Deploy worker
az webapp deploy \
  --name $WORKER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --src-path worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar

echo "Worker deployed!"
```

## Step 4: Verify Deployment

### Check Web App Status
```bash
az webapp browse --name $APP_NAME --resource-group $RESOURCE_GROUP
```

### View Logs
```bash
# Enable logging
az webapp log config \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --application-logging filesystem \
  --level information

# Stream logs
az webapp log tail \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP
```

### Test Upload
1. Navigate to the web application URL
2. Upload a test image
3. Verify the image appears in the list
4. Check Azure Blob Storage for the uploaded file
5. Check worker logs for thumbnail generation

## Step 5: Production Considerations

### Security
- Use Azure Key Vault for sensitive configuration
- Enable HTTPS only
- Configure CORS properly
- Review network security groups

### Monitoring
```bash
# Enable Application Insights
az monitor app-insights component create \
  --app asset-manager-insights \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP

# Link to web app
AI_KEY=$(az monitor app-insights component show \
  --app asset-manager-insights \
  --resource-group $RESOURCE_GROUP \
  --query instrumentationKey \
  --output tsv)

az webapp config appsettings set \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings APPINSIGHTS_INSTRUMENTATIONKEY=$AI_KEY
```

### Scaling
```bash
# Auto-scale based on CPU
az monitor autoscale create \
  --resource-group $RESOURCE_GROUP \
  --resource $APP_NAME \
  --resource-type Microsoft.Web/sites \
  --name autoscale-cpu \
  --min-count 1 \
  --max-count 5 \
  --count 1

az monitor autoscale rule create \
  --resource-group $RESOURCE_GROUP \
  --autoscale-name autoscale-cpu \
  --condition "CpuPercentage > 70 avg 5m" \
  --scale out 1
```

## Troubleshooting

### Check Managed Identity
```bash
# Verify managed identity is enabled
az webapp identity show \
  --name $APP_NAME \
  --resource-group $RESOURCE_GROUP

# Verify role assignments
az role assignment list \
  --assignee $PRINCIPAL_ID
```

### Storage Connection Issues
```bash
# Test storage access from Azure CLI
az storage blob list \
  --container-name $CONTAINER_NAME \
  --account-name $STORAGE_ACCOUNT \
  --auth-mode login
```

### Database Connection Issues
```bash
# Test database connection
psql "host=${DB_SERVER}.postgres.database.azure.com \
      port=5432 \
      dbname=${DB_NAME} \
      user=${DB_ADMIN} \
      password=${DB_PASSWORD} \
      sslmode=require"
```

## Clean Up Resources
```bash
# Delete everything
az group delete --name $RESOURCE_GROUP --yes --no-wait
```

## Next Steps
- Implement CI/CD with Azure DevOps or GitHub Actions
- Migrate from RabbitMQ to Azure Service Bus
- Use Azure CDN for static assets
- Implement Azure Front Door for global distribution
