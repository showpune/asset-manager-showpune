# Azure Blob Storage Migration Guide

This guide provides detailed instructions for migrating from AWS S3 to Azure Blob Storage and deploying the application to Azure.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Resource Setup](#azure-resource-setup)
3. [Configuration](#configuration)
4. [Deployment Options](#deployment-options)
5. [Testing the Migration](#testing-the-migration)
6. [Troubleshooting](#troubleshooting)
7. [Data Migration](#data-migration)

---

## Prerequisites

### Required Tools
- Azure CLI (version 2.0 or later)
- Maven 3.x
- Java 17 JDK

### Azure Subscription
- Active Azure subscription
- Appropriate permissions to create resources and assign roles

---

## Azure Resource Setup

### 1. Create Azure Storage Account

```bash
# Set variables
RESOURCE_GROUP="asset-manager-rg"
LOCATION="eastus"
STORAGE_ACCOUNT="assetmanagerstorage"  # Must be globally unique
CONTAINER_NAME="assets-container"

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create storage account
az storage account create \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_LRS \
  --kind StorageV2

# Create container
az storage container create \
  --name $CONTAINER_NAME \
  --account-name $STORAGE_ACCOUNT \
  --auth-mode login
```

### 2. Configure Managed Identity

#### For Azure App Service:

```bash
# Create App Service Plan
az appservice plan create \
  --name asset-manager-plan \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku B1 \
  --is-linux

# Create Web App with managed identity
az webapp create \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --plan asset-manager-plan \
  --runtime "JAVA:17-java17" \
  --assign-identity [system]

# Get the managed identity principal ID
PRINCIPAL_ID=$(az webapp identity show \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --query principalId -o tsv)

# Assign Storage Blob Data Contributor role
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<subscription-id>/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Storage/storageAccounts/$STORAGE_ACCOUNT
```

#### For Azure Container Apps:

```bash
# Create Container Apps environment
az containerapp env create \
  --name asset-manager-env \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Create Container App with managed identity
az containerapp create \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --environment asset-manager-env \
  --image <your-container-registry>/asset-manager-web:latest \
  --system-assigned

# Get the managed identity principal ID
PRINCIPAL_ID=$(az containerapp identity show \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --query principalId -o tsv)

# Assign Storage Blob Data Contributor role
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<subscription-id>/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Storage/storageAccounts/$STORAGE_ACCOUNT
```

---

## Configuration

### Application Properties

Create or update `application-azure.properties`:

```properties
# Azure Blob Storage Configuration
azure.storage.account-name=${AZURE_STORAGE_ACCOUNT_NAME}
azure.storage.container-name=${AZURE_STORAGE_CONTAINER_NAME}

# Spring Profile
spring.profiles.active=azure

# Database configuration (use Azure Database for PostgreSQL)
spring.datasource.url=jdbc:postgresql://${POSTGRES_SERVER}:5432/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}

# RabbitMQ configuration (use Azure Service Bus or RabbitMQ)
spring.rabbitmq.host=${RABBITMQ_HOST}
spring.rabbitmq.port=5672
spring.rabbitmq.username=${RABBITMQ_USER}
spring.rabbitmq.password=${RABBITMQ_PASSWORD}
```

### Environment Variables

Set the following environment variables in your Azure deployment:

```bash
# For Azure App Service
az webapp config appsettings set \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --settings \
    AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT \
    AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME \
    SPRING_PROFILES_ACTIVE=azure \
    POSTGRES_SERVER=<your-postgres-server> \
    POSTGRES_DB=<your-database> \
    POSTGRES_USER=<your-username> \
    POSTGRES_PASSWORD=<your-password> \
    RABBITMQ_HOST=<your-rabbitmq-host>
```

---

## Deployment Options

### Option 1: Deploy as JAR to Azure App Service

```bash
# Build the application
mvn clean package -DskipTests

# Deploy web module
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-web \
  --src-path web/target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar

# Deploy worker module (as a separate App Service or WebJob)
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-worker \
  --src-path worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar
```

### Option 2: Deploy as Container

```bash
# Build Docker images (create Dockerfiles first)
docker build -t <your-registry>/asset-manager-web:latest ./web
docker build -t <your-registry>/asset-manager-worker:latest ./worker

# Push to Azure Container Registry
az acr login --name <your-registry>
docker push <your-registry>/asset-manager-web:latest
docker push <your-registry>/asset-manager-worker:latest

# Deploy to Azure Container Apps
az containerapp update \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --image <your-registry>/asset-manager-web:latest
```

---

## Testing the Migration

### Local Testing with Azure Blob Storage

1. Install Azure CLI and sign in:
   ```bash
   az login
   ```

2. Set environment variables:
   ```bash
   export AZURE_STORAGE_ACCOUNT_NAME=<storage-account-name>
   export AZURE_STORAGE_CONTAINER_NAME=<container-name>
   export SPRING_PROFILES_ACTIVE=azure
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run -pl web
   ```

4. Test upload functionality:
   - Navigate to http://localhost:8080
   - Upload an image file
   - Verify the file appears in Azure Blob Storage
   - Verify thumbnail generation

### Verify Azure Deployment

1. Check application logs:
   ```bash
   az webapp log tail --name asset-manager-web --resource-group $RESOURCE_GROUP
   ```

2. Test endpoints:
   ```bash
   # List files
   curl https://asset-manager-web.azurewebsites.net/s3/list
   
   # Upload file
   curl -X POST -F "file=@test-image.jpg" https://asset-manager-web.azurewebsites.net/s3/upload
   ```

3. Verify in Azure Portal:
   - Navigate to Storage Account → Containers → assets-container
   - Confirm files are being stored correctly
   - Check blob properties and metadata

---

## Troubleshooting

### Issue: "DefaultAzureCredential authentication failed"

**Cause**: Managed identity not properly configured or insufficient permissions.

**Solution**:
1. Verify managed identity is enabled:
   ```bash
   az webapp identity show --name asset-manager-web --resource-group $RESOURCE_GROUP
   ```

2. Verify role assignment:
   ```bash
   az role assignment list --assignee <principal-id> --all
   ```

3. Ensure "Storage Blob Data Contributor" role is assigned at storage account level.

### Issue: "Container not found"

**Cause**: Container doesn't exist or name mismatch.

**Solution**:
1. Verify container exists:
   ```bash
   az storage container list --account-name $STORAGE_ACCOUNT --auth-mode login
   ```

2. Check environment variable:
   ```bash
   az webapp config appsettings list --name asset-manager-web --resource-group $RESOURCE_GROUP
   ```

3. Create container if missing:
   ```bash
   az storage container create --name $CONTAINER_NAME --account-name $STORAGE_ACCOUNT --auth-mode login
   ```

### Issue: "Build fails with Azure SDK errors"

**Cause**: Dependency conflicts or missing dependencies.

**Solution**:
1. Clean and rebuild:
   ```bash
   mvn clean install -U
   ```

2. Verify Azure SDK versions in pom.xml:
   ```xml
   <azure-storage.version>12.29.0</azure-storage.version>
   ```

### Issue: "Tests fail in Azure environment"

**Cause**: Profile mismatch or missing test configuration.

**Solution**:
1. Ensure test profile is set to "dev":
   ```properties
   # application.properties in src/test/resources
   spring.profiles.active=dev
   ```

2. Run tests with specific profile:
   ```bash
   mvn test -Dspring.profiles.active=dev
   ```

---

## Data Migration

### Migrating Existing Data from S3 to Azure Blob Storage

#### Using AzCopy (Recommended)

1. Install AzCopy:
   ```bash
   # Linux
   wget https://aka.ms/downloadazcopy-v10-linux
   tar -xvf downloadazcopy-v10-linux
   sudo cp ./azcopy_linux_amd64_*/azcopy /usr/bin/
   ```

2. Generate SAS token for Azure Blob Storage:
   ```bash
   END_DATE=$(date -u -d "30 days" '+%Y-%m-%dT%H:%MZ')
   SAS_TOKEN=$(az storage container generate-sas \
     --name $CONTAINER_NAME \
     --account-name $STORAGE_ACCOUNT \
     --permissions acdlrw \
     --expiry $END_DATE \
     --auth-mode login \
     --as-user \
     -o tsv)
   ```

3. Copy data from S3 to Azure:
   ```bash
   # Set AWS credentials
   export AWS_ACCESS_KEY_ID=<your-access-key>
   export AWS_SECRET_ACCESS_KEY=<your-secret-key>
   
   # Copy data
   azcopy copy \
     "https://s3.amazonaws.com/<bucket-name>/*" \
     "https://${STORAGE_ACCOUNT}.blob.core.windows.net/${CONTAINER_NAME}?${SAS_TOKEN}" \
     --recursive=true
   ```

#### Using Custom Migration Script

For complex migrations with metadata preservation:

```bash
# Run migration script (create custom script based on your needs)
java -jar migration-tool.jar \
  --source s3 \
  --source-bucket <s3-bucket> \
  --target azure \
  --target-account $STORAGE_ACCOUNT \
  --target-container $CONTAINER_NAME
```

### Database Migration

If you have metadata in the database:

1. Export from current database:
   ```bash
   pg_dump -h <current-host> -U <user> -d <database> -t image_metadata > metadata.sql
   ```

2. Import to Azure PostgreSQL:
   ```bash
   psql -h <azure-postgres-server> -U <user> -d <database> < metadata.sql
   ```

3. Update S3 URLs in database if needed:
   ```sql
   UPDATE image_metadata 
   SET s3_url = REPLACE(s3_url, 's3.amazonaws.com/<bucket>', '<storage-account>.blob.core.windows.net/<container>');
   ```

---

## Performance Considerations

### Blob Storage Tiers

Choose appropriate storage tier based on access patterns:

- **Hot tier**: Frequently accessed data (default for active assets)
- **Cool tier**: Infrequently accessed data (older thumbnails)
- **Archive tier**: Rarely accessed data (historical backups)

```bash
# Set default access tier to hot
az storage account update \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --access-tier Hot
```

### CDN Integration

For better performance with global users:

```bash
# Create CDN profile
az cdn profile create \
  --name asset-manager-cdn \
  --resource-group $RESOURCE_GROUP \
  --sku Standard_Microsoft

# Create CDN endpoint
az cdn endpoint create \
  --name asset-manager-endpoint \
  --profile-name asset-manager-cdn \
  --resource-group $RESOURCE_GROUP \
  --origin ${STORAGE_ACCOUNT}.blob.core.windows.net \
  --origin-host-header ${STORAGE_ACCOUNT}.blob.core.windows.net
```

---

## Security Best Practices

1. **Use Managed Identity**: Never store credentials in code or configuration
2. **Enable HTTPS Only**: Enforce secure connections
   ```bash
   az storage account update \
     --name $STORAGE_ACCOUNT \
     --resource-group $RESOURCE_GROUP \
     --https-only true
   ```

3. **Configure Network Rules**: Restrict access to specific VNets
   ```bash
   az storage account network-rule add \
     --account-name $STORAGE_ACCOUNT \
     --resource-group $RESOURCE_GROUP \
     --vnet-name <vnet-name> \
     --subnet <subnet-name>
   ```

4. **Enable Soft Delete**: Protect against accidental deletion
   ```bash
   az storage blob service-properties delete-policy update \
     --account-name $STORAGE_ACCOUNT \
     --enable true \
     --days-retained 7
   ```

5. **Monitor Access**: Enable diagnostic logs
   ```bash
   az monitor diagnostic-settings create \
     --resource /subscriptions/<subscription-id>/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.Storage/storageAccounts/$STORAGE_ACCOUNT \
     --name storage-logs \
     --logs '[{"category": "StorageRead", "enabled": true}, {"category": "StorageWrite", "enabled": true}]' \
     --workspace <log-analytics-workspace-id>
   ```

---

## Support and Resources

- [Azure Blob Storage Documentation](https://docs.microsoft.com/en-us/azure/storage/blobs/)
- [Azure Managed Identity Documentation](https://docs.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/)
- [Spring Cloud Azure Documentation](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/)
- [Azure SDK for Java](https://github.com/Azure/azure-sdk-for-java)

---

## Rollback Plan

If you need to rollback to AWS S3:

1. Switch profile back to AWS:
   ```bash
   az webapp config appsettings set \
     --name asset-manager-web \
     --resource-group $RESOURCE_GROUP \
     --settings SPRING_PROFILES_ACTIVE=aws
   ```

2. Ensure AWS credentials are configured
3. Restart application

The application maintains backward compatibility with AWS S3 through profile-based configuration.
