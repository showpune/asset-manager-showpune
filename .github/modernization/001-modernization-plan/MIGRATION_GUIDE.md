# Azure Migration Guide

This guide provides step-by-step instructions for deploying the migrated Asset Manager application to Azure.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Resource Provisioning](#azure-resource-provisioning)
3. [Configuration](#configuration)
4. [Data Migration](#data-migration)
5. [Deployment](#deployment)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Azure Resources
- Azure Subscription
- Azure CLI installed and configured (`az login`)
- Permissions to create resources and assign RBAC roles

### Application Requirements
- Java 17 runtime
- Maven 3.x (for building)
- Spring Boot 3.2.2

---

## Azure Resource Provisioning

### 1. Create Resource Group

```bash
# Set variables
export RESOURCE_GROUP="asset-manager-rg"
export LOCATION="eastus"
export STORAGE_ACCOUNT="assetmanagerstorage"
export CONTAINER_NAME="images"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### 2. Create Azure Storage Account

```bash
# Create storage account
az storage account create \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku Standard_LRS \
  --kind StorageV2 \
  --allow-blob-public-access false

# Create blob container
az storage container create \
  --name $CONTAINER_NAME \
  --account-name $STORAGE_ACCOUNT \
  --auth-mode login
```

### 3. Choose Deployment Target

Select one of the following deployment options:

#### Option A: Azure App Service (Recommended for simplicity)

```bash
export APP_SERVICE_PLAN="asset-manager-plan"
export WEB_APP_NAME="asset-manager-web"

# Create App Service Plan
az appservice plan create \
  --name $APP_SERVICE_PLAN \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --sku B1 \
  --is-linux

# Create Web App
az webapp create \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --plan $APP_SERVICE_PLAN \
  --runtime "JAVA:17-java17"

# Enable managed identity
az webapp identity assign \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP
```

#### Option B: Azure Container Apps

```bash
export CONTAINER_APP_ENV="asset-manager-env"
export CONTAINER_APP_NAME="asset-manager-app"

# Create Container Apps environment
az containerapp env create \
  --name $CONTAINER_APP_ENV \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Note: Container image deployment requires Docker image
# See Deployment section for container build instructions
```

#### Option C: Azure Kubernetes Service (AKS)

```bash
export AKS_CLUSTER="asset-manager-aks"

# Create AKS cluster
az aks create \
  --name $AKS_CLUSTER \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION \
  --node-count 2 \
  --enable-managed-identity \
  --generate-ssh-keys

# Get credentials
az aks get-credentials \
  --name $AKS_CLUSTER \
  --resource-group $RESOURCE_GROUP
```

### 4. Assign RBAC Roles

Get the managed identity principal ID:

```bash
# For App Service
export PRINCIPAL_ID=$(az webapp identity show \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query principalId \
  --output tsv)

# For Container Apps (after deployment)
export PRINCIPAL_ID=$(az containerapp identity show \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query principalId \
  --output tsv)

# For AKS (use workload identity)
# Follow: https://learn.microsoft.com/azure/aks/workload-identity-overview
```

Assign Storage Blob Data Contributor role:

```bash
export STORAGE_ACCOUNT_ID=$(az storage account show \
  --name $STORAGE_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query id \
  --output tsv)

az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ACCOUNT_ID
```

---

## Configuration

### Environment Variables

Set the following environment variables in your deployment target:

#### Required Variables

```bash
# Azure Storage Configuration
AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT
AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME

# Spring Profile
SPRING_PROFILES_ACTIVE=azure

# Database Configuration (if using PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://<server>.postgres.database.azure.com:5432/<database>
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>

# RabbitMQ Configuration (if using)
SPRING_RABBITMQ_HOST=<rabbitmq-host>
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=<username>
SPRING_RABBITMQ_PASSWORD=<password>
```

#### Optional Variables

```bash
# Local storage path (for dev profile only)
LOCAL_STORAGE_DIRECTORY=/tmp/storage

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_MICROSOFT_MIGRATION=DEBUG
```

### Set Environment Variables in App Service

```bash
az webapp config appsettings set \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT \
    AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME \
    SPRING_PROFILES_ACTIVE=azure
```

### Set Environment Variables in Container Apps

```bash
az containerapp update \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars \
    AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT \
    AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME \
    SPRING_PROFILES_ACTIVE=azure
```

---

## Data Migration

### Option 1: AzCopy (Recommended)

```bash
# Install AzCopy
# Download from: https://aka.ms/downloadazcopy

# Login with Azure AD
azcopy login

# Copy from S3 to Azure Blob Storage
# First, download from S3 to local
aws s3 sync s3://<source-bucket> ./local-data

# Then upload to Azure
azcopy copy './local-data/*' \
  "https://$STORAGE_ACCOUNT.blob.core.windows.net/$CONTAINER_NAME/" \
  --recursive
```

### Option 2: Azure Data Factory

1. Create Azure Data Factory instance
2. Create linked services for S3 and Azure Blob Storage
3. Create a copy pipeline
4. Configure source and sink
5. Run the pipeline

### Option 3: Custom Migration Script

```bash
# Build the application
./mvnw clean package -DskipTests

# Run migration script (if implemented)
java -jar target/assets-manager-web-*.jar \
  --spring.profiles.active=aws,azure \
  --migration.mode=true
```

---

## Deployment

### Deploy to App Service

```bash
# Build the application
./mvnw clean package -DskipTests

# Deploy JAR file
az webapp deploy \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --src-path web/target/assets-manager-web-*.jar \
  --type jar

# Restart the app
az webapp restart \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP
```

### Deploy to Container Apps

```bash
# Build Docker image
docker build -t $CONTAINER_APP_NAME:latest .

# Push to Azure Container Registry
export ACR_NAME="assetmanageracr"

az acr create \
  --name $ACR_NAME \
  --resource-group $RESOURCE_GROUP \
  --sku Basic

az acr login --name $ACR_NAME

docker tag $CONTAINER_APP_NAME:latest \
  $ACR_NAME.azurecr.io/$CONTAINER_APP_NAME:latest

docker push $ACR_NAME.azurecr.io/$CONTAINER_APP_NAME:latest

# Create Container App
az containerapp create \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment $CONTAINER_APP_ENV \
  --image $ACR_NAME.azurecr.io/$CONTAINER_APP_NAME:latest \
  --registry-server $ACR_NAME.azurecr.io \
  --target-port 8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 10 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --env-vars \
    AZURE_STORAGE_ACCOUNT_NAME=$STORAGE_ACCOUNT \
    AZURE_STORAGE_CONTAINER_NAME=$CONTAINER_NAME \
    SPRING_PROFILES_ACTIVE=azure
```

### Deploy to AKS

```bash
# Apply Kubernetes manifests
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml

# Configure workload identity
# Follow: https://learn.microsoft.com/azure/aks/workload-identity-deploy-cluster
```

---

## Verification

### 1. Check Application Health

```bash
# For App Service
curl https://$WEB_APP_NAME.azurewebsites.net/actuator/health

# For Container Apps
export APP_URL=$(az containerapp show \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query properties.configuration.ingress.fqdn \
  --output tsv)

curl https://$APP_URL/actuator/health
```

### 2. Test File Upload

```bash
# Upload a test file
curl -X POST https://$WEB_APP_NAME.azurewebsites.net/s3/upload \
  -F "file=@test-image.jpg" \
  -H "Content-Type: multipart/form-data"
```

### 3. Verify Storage

```bash
# List blobs in container
az storage blob list \
  --container-name $CONTAINER_NAME \
  --account-name $STORAGE_ACCOUNT \
  --auth-mode login
```

### 4. Check Logs

```bash
# App Service logs
az webapp log tail \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP

# Container Apps logs
az containerapp logs show \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP
```

---

## Troubleshooting

### Common Issues

#### 1. "No qualifying bean of type StorageService"

**Cause:** Profile not set correctly  
**Solution:** Ensure `SPRING_PROFILES_ACTIVE=azure` is set

```bash
az webapp config appsettings set \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings SPRING_PROFILES_ACTIVE=azure
```

#### 2. "403 Forbidden" or "Access Denied" errors

**Cause:** Missing RBAC permissions  
**Solution:** Verify managed identity has Storage Blob Data Contributor role

```bash
# List role assignments
az role assignment list \
  --assignee $PRINCIPAL_ID \
  --scope $STORAGE_ACCOUNT_ID
```

#### 3. "DefaultAzureCredential failed to retrieve token"

**Cause:** Managed identity not properly configured  
**Solution:** Verify managed identity is enabled

```bash
# Check identity status
az webapp identity show \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP
```

#### 4. Application fails to start

**Cause:** Missing environment variables  
**Solution:** Check all required variables are set

```bash
# List current settings
az webapp config appsettings list \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP
```

### Enable Debug Logging

```bash
az webapp config appsettings set \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    LOGGING_LEVEL_COM_MICROSOFT_MIGRATION=DEBUG \
    LOGGING_LEVEL_COM_AZURE=DEBUG
```

---

## Rollback Procedure

If issues occur, rollback to S3:

```bash
# Set profile to aws
az webapp config appsettings set \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings SPRING_PROFILES_ACTIVE=aws

# Configure AWS credentials
az webapp config appsettings set \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    AWS_ACCESS_KEY_ID=<key-id> \
    AWS_SECRET_ACCESS_KEY=<secret> \
    AWS_REGION=<region> \
    AWS_S3_BUCKET=<bucket-name>

# Restart
az webapp restart \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP
```

---

## Monitoring and Optimization

### Enable Application Insights

```bash
export APP_INSIGHTS="asset-manager-insights"

# Create Application Insights
az monitor app-insights component create \
  --app $APP_INSIGHTS \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP

# Get instrumentation key
export INSTRUMENTATION_KEY=$(az monitor app-insights component show \
  --app $APP_INSIGHTS \
  --resource-group $RESOURCE_GROUP \
  --query instrumentationKey \
  --output tsv)

# Configure in App Service
az webapp config appsettings set \
  --name $WEB_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --settings \
    APPLICATIONINSIGHTS_CONNECTION_STRING="InstrumentationKey=$INSTRUMENTATION_KEY"
```

### Configure Storage Metrics

```bash
# Enable storage analytics
az storage logging update \
  --account-name $STORAGE_ACCOUNT \
  --services b \
  --log rwd \
  --retention 7 \
  --auth-mode login
```

---

## Support

For additional support:
- Azure Documentation: https://docs.microsoft.com/azure
- Spring Boot on Azure: https://docs.microsoft.com/java/azure/spring-framework
- Azure Storage Documentation: https://docs.microsoft.com/azure/storage

---

**Last Updated:** 2026-02-09
