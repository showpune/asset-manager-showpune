# Azure Migration Guide

## Overview
This guide provides step-by-step instructions for deploying the modernized application to Azure using managed identity authentication.

## Prerequisites
1. Azure subscription
2. Azure CLI installed
3. Java 21 JDK installed
4. Maven installed

## Azure Resources Required

### 1. Azure Blob Storage
**Purpose**: Store images (originals and thumbnails)

**Setup Steps**:
```bash
# Create a resource group
az group create --name asset-manager-rg --location eastus

# Create a storage account
az storage account create \
  --name assetmanagerstorage \
  --resource-group asset-manager-rg \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2

# Create a container for images
az storage container create \
  --name images \
  --account-name assetmanagerstorage \
  --auth-mode login
```

**Configuration**:
- Endpoint: `https://assetmanagerstorage.blob.core.windows.net`
- Container name: `images`

### 2. Azure Service Bus
**Purpose**: Message queue for image processing tasks

**Setup Steps**:
```bash
# Create a Service Bus namespace
az servicebus namespace create \
  --name asset-manager-bus \
  --resource-group asset-manager-rg \
  --location eastus \
  --sku Standard

# Create a queue
az servicebus queue create \
  --name image-processing \
  --namespace-name asset-manager-bus \
  --resource-group asset-manager-rg
```

**Configuration**:
- Namespace: `asset-manager-bus`
- Queue name: `image-processing`

### 3. Azure Database for PostgreSQL
**Purpose**: Store image metadata

**Setup Steps**:
```bash
# Create PostgreSQL server
az postgres flexible-server create \
  --name asset-manager-db \
  --resource-group asset-manager-rg \
  --location eastus \
  --admin-user adminuser \
  --admin-password <SecurePassword123!> \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32

# Create database
az postgres flexible-server db create \
  --resource-group asset-manager-rg \
  --server-name asset-manager-db \
  --database-name assets_manager
```

## Deployment Options

### Option 1: Deploy to Azure App Service (Recommended)

#### Deploy Web Application
```bash
# Create App Service Plan
az appservice plan create \
  --name asset-manager-plan \
  --resource-group asset-manager-rg \
  --location eastus \
  --sku B1 \
  --is-linux

# Create Web App
az webapp create \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --plan asset-manager-plan \
  --runtime "JAVA:21-java21"

# Enable managed identity
az webapp identity assign \
  --name asset-manager-web \
  --resource-group asset-manager-rg

# Configure environment variables
az webapp config appsettings set \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    AZURE_STORAGE_ACCOUNT_ENDPOINT=https://assetmanagerstorage.blob.core.windows.net \
    AZURE_STORAGE_CONTAINER_NAME=images \
    AZURE_SERVICEBUS_NAMESPACE=asset-manager-bus \
    AZURE_SERVICEBUS_QUEUE_NAME=image-processing \
    SPRING_DATASOURCE_URL=jdbc:postgresql://asset-manager-db.postgres.database.azure.com:5432/assets_manager \
    SPRING_DATASOURCE_USERNAME=adminuser \
    SPRING_DATASOURCE_PASSWORD=<SecurePassword123!>

# Deploy the application
cd web
mvn clean package -DskipTests
az webapp deploy \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --src-path target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar
```

#### Deploy Worker Application
```bash
# Create Web App for worker
az webapp create \
  --name asset-manager-worker \
  --resource-group asset-manager-rg \
  --plan asset-manager-plan \
  --runtime "JAVA:21-java21"

# Enable managed identity
az webapp identity assign \
  --name asset-manager-worker \
  --resource-group asset-manager-rg

# Configure environment variables
az webapp config appsettings set \
  --name asset-manager-worker \
  --resource-group asset-manager-rg \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    AZURE_STORAGE_ACCOUNT_ENDPOINT=https://assetmanagerstorage.blob.core.windows.net \
    AZURE_STORAGE_CONTAINER_NAME=images \
    AZURE_SERVICEBUS_NAMESPACE=asset-manager-bus \
    AZURE_SERVICEBUS_QUEUE_NAME=image-processing \
    SPRING_DATASOURCE_URL=jdbc:postgresql://asset-manager-db.postgres.database.azure.com:5432/assets_manager \
    SPRING_DATASOURCE_USERNAME=adminuser \
    SPRING_DATASOURCE_PASSWORD=<SecurePassword123!>

# Deploy the application
cd worker
mvn clean package -DskipTests
az webapp deploy \
  --name asset-manager-worker \
  --resource-group asset-manager-rg \
  --src-path target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar
```

## Configure Managed Identity Permissions

After deploying the applications, configure RBAC permissions:

### Blob Storage Permissions
```bash
# Get the managed identity principal ID for web app
WEB_PRINCIPAL_ID=$(az webapp identity show \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --query principalId -o tsv)

# Get the managed identity principal ID for worker app
WORKER_PRINCIPAL_ID=$(az webapp identity show \
  --name asset-manager-worker \
  --resource-group asset-manager-rg \
  --query principalId -o tsv)

# Get the storage account ID
STORAGE_ID=$(az storage account show \
  --name assetmanagerstorage \
  --resource-group asset-manager-rg \
  --query id -o tsv)

# Assign Storage Blob Data Contributor role to web app
az role assignment create \
  --assignee $WEB_PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID

# Assign Storage Blob Data Contributor role to worker app
az role assignment create \
  --assignee $WORKER_PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID
```

### Service Bus Permissions
```bash
# Get the Service Bus namespace ID
SERVICEBUS_ID=$(az servicebus namespace show \
  --name asset-manager-bus \
  --resource-group asset-manager-rg \
  --query id -o tsv)

# Assign Azure Service Bus Data Sender role to web app
az role assignment create \
  --assignee $WEB_PRINCIPAL_ID \
  --role "Azure Service Bus Data Sender" \
  --scope $SERVICEBUS_ID

# Assign Azure Service Bus Data Receiver role to worker app
az role assignment create \
  --assignee $WORKER_PRINCIPAL_ID \
  --role "Azure Service Bus Data Receiver" \
  --scope $SERVICEBUS_ID
```

## Testing the Deployment

1. Navigate to the web application URL: `https://asset-manager-web.azurewebsites.net`
2. Upload an image
3. Verify the image appears in the list
4. Check that the thumbnail is generated (by the worker)
5. View and delete images to test all functionality

## Monitoring

View logs using Azure CLI:
```bash
# Web app logs
az webapp log tail --name asset-manager-web --resource-group asset-manager-rg

# Worker app logs
az webapp log tail --name asset-manager-worker --resource-group asset-manager-rg
```

## Troubleshooting

### Common Issues

1. **Authentication Errors**
   - Ensure managed identity is enabled on the App Service
   - Verify RBAC role assignments are correctly configured
   - Check that role assignments have propagated (can take up to 5 minutes)

2. **Connection Issues**
   - Verify environment variables are correctly set
   - Check that Azure resources are in the same region or properly networked
   - Ensure PostgreSQL firewall allows Azure services

3. **Image Processing Not Working**
   - Check worker application logs
   - Verify Service Bus queue has messages
   - Ensure worker has proper permissions on Service Bus and Blob Storage

## Cost Optimization Tips

1. Use Azure Reserved Capacity for predictable workloads
2. Enable auto-scaling only if needed
3. Consider Azure Blob Storage lifecycle management for old files
4. Use appropriate SKUs (B1 for development, S1+ for production)

## Security Best Practices

1. ✅ **Managed Identity**: No credentials stored in code or configuration
2. ✅ **RBAC**: Fine-grained access control
3. Enable Azure Key Vault for database passwords
4. Enable HTTPS only for web apps
5. Configure Azure Monitor alerts for security events
6. Regularly review and audit access logs

## Rollback Procedure

If issues occur after migration:

1. Switch back to AWS by changing the Spring profile:
   ```bash
   az webapp config appsettings set \
     --name asset-manager-web \
     --resource-group asset-manager-rg \
     --settings SPRING_PROFILES_ACTIVE=default
   ```

2. Or deploy the previous version of the application

## Support

For issues or questions:
- Check Azure Status: https://status.azure.com/
- Review application logs in Azure Portal
- Contact Azure Support if needed
