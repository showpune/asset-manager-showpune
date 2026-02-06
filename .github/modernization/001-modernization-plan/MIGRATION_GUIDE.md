# Migration Guide: AWS to Azure

This guide provides step-by-step instructions for migrating the Asset Manager application from AWS to Azure.

## Prerequisites

- Azure subscription with appropriate permissions
- Azure CLI installed (`az cli`)
- Java 21 JDK installed
- Maven installed

## Step 1: Create Azure Resources

### 1.1 Create Resource Group
```bash
az group create \
  --name asset-manager-rg \
  --location eastus
```

### 1.2 Create Storage Account
```bash
az storage account create \
  --name assetmanagerstg \
  --resource-group asset-manager-rg \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2
```

### 1.3 Create Blob Container
```bash
az storage container create \
  --name assets \
  --account-name assetmanagerstg \
  --auth-mode login
```

### 1.4 Create Service Bus Namespace
```bash
az servicebus namespace create \
  --name asset-manager-sb \
  --resource-group asset-manager-rg \
  --location eastus \
  --sku Standard
```

### 1.5 Create Service Bus Queue
```bash
az servicebus queue create \
  --name image-processing-queue \
  --namespace-name asset-manager-sb \
  --resource-group asset-manager-rg
```

### 1.6 Create Azure Database for PostgreSQL (Optional)
```bash
az postgres flexible-server create \
  --name asset-manager-db \
  --resource-group asset-manager-rg \
  --location eastus \
  --admin-user assetadmin \
  --admin-password '<YourSecurePassword>' \
  --sku-name Standard_B2s \
  --version 14
```

## Step 2: Configure Managed Identity

### 2.1 For Azure App Service

#### Create App Service Plan
```bash
az appservice plan create \
  --name asset-manager-plan \
  --resource-group asset-manager-rg \
  --location eastus \
  --sku B1 \
  --is-linux
```

#### Create Web App
```bash
az webapp create \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --plan asset-manager-plan \
  --runtime "JAVA:21-java21"
```

#### Enable System-Assigned Managed Identity
```bash
# For web module
az webapp identity assign \
  --name asset-manager-web \
  --resource-group asset-manager-rg

# For worker module (if separate app)
az webapp identity assign \
  --name asset-manager-worker \
  --resource-group asset-manager-rg
```

### 2.2 For Azure Container Apps (Alternative)

#### Create Container Apps Environment
```bash
az containerapp env create \
  --name asset-manager-env \
  --resource-group asset-manager-rg \
  --location eastus
```

#### Create Container App with Managed Identity
```bash
az containerapp create \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --environment asset-manager-env \
  --image <your-registry>/asset-manager-web:latest \
  --system-assigned
```

## Step 3: Assign RBAC Roles

### 3.1 Get Managed Identity Principal ID
```bash
# For App Service
WEB_IDENTITY=$(az webapp identity show \
  --name asset-manager-web \
  --resource-group asset-manager-rg \
  --query principalId -o tsv)

WORKER_IDENTITY=$(az webapp identity show \
  --name asset-manager-worker \
  --resource-group asset-manager-rg \
  --query principalId -o tsv)
```

### 3.2 Assign Storage Blob Data Contributor Role
```bash
STORAGE_ID=$(az storage account show \
  --name assetmanagerstg \
  --resource-group asset-manager-rg \
  --query id -o tsv)

# For web app
az role assignment create \
  --assignee $WEB_IDENTITY \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID

# For worker app
az role assignment create \
  --assignee $WORKER_IDENTITY \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID
```

### 3.3 Assign Service Bus Roles
```bash
SERVICEBUS_ID=$(az servicebus namespace show \
  --name asset-manager-sb \
  --resource-group asset-manager-rg \
  --query id -o tsv)

# For web app (sender)
az role assignment create \
  --assignee $WEB_IDENTITY \
  --role "Azure Service Bus Data Sender" \
  --scope $SERVICEBUS_ID

# For worker app (receiver)
az role assignment create \
  --assignee $WORKER_IDENTITY \
  --role "Azure Service Bus Data Receiver" \
  --scope $SERVICEBUS_ID
```

## Step 4: Update Application Configuration

### 4.1 Update Web Module Configuration

Update `application-azure.properties`:
```properties
spring.application.name=assets-manager

# Azure Blob Storage Configuration
azure.storage.account-name=assetmanagerstg
azure.storage.container-name=assets

# Azure Service Bus Configuration
spring.cloud.azure.servicebus.namespace=asset-manager-sb
spring.cloud.azure.servicebus.credential.managed-identity-enabled=true
azure.servicebus.queue-name=image-processing-queue

# Database Configuration
spring.datasource.url=jdbc:postgresql://asset-manager-db.postgres.database.azure.com:5432/assets_manager
spring.datasource.username=assetadmin
spring.datasource.password=<YourSecurePassword>
```

### 4.2 Update Worker Module Configuration

Update `application-azure.properties`:
```properties
spring.application.name=assets-manager-worker

# Azure Blob Storage Configuration
azure.storage.account-name=assetmanagerstg
azure.storage.container-name=assets

# Azure Service Bus Configuration
spring.cloud.azure.servicebus.namespace=asset-manager-sb
spring.cloud.azure.servicebus.credential.managed-identity-enabled=true
azure.servicebus.queue-name=image-processing-queue

# Database Configuration
spring.datasource.url=jdbc:postgresql://asset-manager-db.postgres.database.azure.com:5432/assets_manager
spring.datasource.username=assetadmin
spring.datasource.password=<YourSecurePassword>
```

## Step 5: Build Application

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
./mvnw clean package -DskipTests
```

## Step 6: Deploy to Azure

### 6.1 Deploy Web Module

```bash
az webapp deploy \
  --resource-group asset-manager-rg \
  --name asset-manager-web \
  --src-path web/target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar
```

### 6.2 Set Application Settings

```bash
az webapp config appsettings set \
  --resource-group asset-manager-rg \
  --name asset-manager-web \
  --settings SPRING_PROFILES_ACTIVE=azure
```

### 6.3 Deploy Worker Module

```bash
az webapp deploy \
  --resource-group asset-manager-rg \
  --name asset-manager-worker \
  --src-path worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar
```

```bash
az webapp config appsettings set \
  --resource-group asset-manager-rg \
  --name asset-manager-worker \
  --settings SPRING_PROFILES_ACTIVE=azure
```

## Step 7: Verify Deployment

### 7.1 Check Application Logs

```bash
# Web module logs
az webapp log tail \
  --resource-group asset-manager-rg \
  --name asset-manager-web

# Worker module logs
az webapp log tail \
  --resource-group asset-manager-rg \
  --name asset-manager-worker
```

### 7.2 Test Image Upload

1. Access the web application URL
2. Upload a test image
3. Verify the image appears in the list
4. Check that thumbnail is generated (may take a moment)

### 7.3 Verify Azure Resources

```bash
# Check blobs in storage
az storage blob list \
  --container-name assets \
  --account-name assetmanagerstg \
  --auth-mode login

# Check Service Bus queue messages
az servicebus queue show \
  --name image-processing-queue \
  --namespace-name asset-manager-sb \
  --resource-group asset-manager-rg \
  --query '{Active:messageCount}'
```

## Step 8: Data Migration (if migrating from AWS)

### 8.1 Export Data from AWS S3

```bash
# Download all objects from S3
aws s3 sync s3://your-aws-bucket ./migration-data/
```

### 8.2 Upload to Azure Blob Storage

```bash
# Upload to Azure
az storage blob upload-batch \
  --destination assets \
  --source ./migration-data/ \
  --account-name assetmanagerstg \
  --auth-mode login
```

### 8.3 Migrate Database

```bash
# Dump PostgreSQL from AWS RDS
pg_dump -h aws-rds-endpoint -U username -d assets_manager > migration.sql

# Restore to Azure PostgreSQL
psql -h asset-manager-db.postgres.database.azure.com -U assetadmin -d assets_manager < migration.sql
```

## Troubleshooting

### Managed Identity Not Working

1. Verify RBAC role assignments:
```bash
az role assignment list --assignee $WEB_IDENTITY --all
```

2. Check application logs for authentication errors

3. Ensure managed identity is enabled:
```bash
az webapp identity show \
  --name asset-manager-web \
  --resource-group asset-manager-rg
```

### Service Bus Connection Issues

1. Verify Service Bus namespace is accessible
2. Check firewall rules if configured
3. Verify queue exists and is active

### Storage Access Issues

1. Verify container exists and has correct permissions
2. Check storage account firewall settings
3. Verify managed identity has appropriate roles

## Rollback Plan

If issues occur, you can switch back to AWS:

1. Stop Azure deployments
2. Update production to use default profile (AWS)
3. Verify AWS resources are still operational
4. Redirect traffic back to AWS deployment

## Cost Optimization

1. **Storage**: Use appropriate storage tier (Hot/Cool/Archive)
2. **Service Bus**: Consider Basic tier if advanced features not needed
3. **Database**: Use appropriate compute tier, enable auto-pause if applicable
4. **App Service**: Use appropriate pricing tier, consider auto-scaling

## Security Best Practices

1. Enable Azure Defender for Storage
2. Enable diagnostic logging for all resources
3. Configure network security rules
4. Use Azure Key Vault for sensitive configuration (database passwords)
5. Enable Azure Monitor for application insights
6. Implement backup and disaster recovery

## Next Steps

- Set up Azure Monitor alerts
- Configure Application Insights for monitoring
- Implement CI/CD pipeline for automated deployments
- Review and optimize Azure costs
- Plan for disaster recovery and backup
