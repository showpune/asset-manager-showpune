# Azure Migration and Deployment Guide

**Project**: Asset Manager Kit  
**Plan**: 001-modernization-plan  
**Last Updated**: February 6, 2026

## Overview

This guide provides step-by-step instructions for deploying the modernized Asset Manager Kit application to Azure. The application now uses Azure Service Bus for messaging and is ready for cloud deployment with managed identity authentication.

---

## Prerequisites

Before deploying, ensure you have:

- ✅ Azure subscription with appropriate permissions
- ✅ Azure CLI installed and configured (`az --version`)
- ✅ Java 17 JDK installed
- ✅ Maven 3.8+ installed
- ✅ Git repository access

---

## Architecture Overview

### Application Components

1. **Web Module** (`/web`)
   - REST API for file upload and management
   - Sends messages to Azure Service Bus queues
   - Requires: Azure Service Bus Data Sender role

2. **Worker Module** (`/worker`)
   - Background processor for file operations
   - Consumes messages from Azure Service Bus queues
   - Requires: Azure Service Bus Data Receiver role

### Azure Resources Required

| Resource | Purpose | Queue Names |
|----------|---------|-------------|
| Azure Service Bus Namespace | Messaging infrastructure | `file-queue`, `backup-queue` |
| Managed Identity | Passwordless authentication | N/A |
| Azure Container Registry (Optional) | Container image storage | N/A |
| Azure App Service / Container Apps | Application hosting | N/A |

---

## Step 1: Create Azure Service Bus Namespace

### 1.1 Create Resource Group (if needed)

```bash
# Set variables
RESOURCE_GROUP="rg-assetmanager-prod"
LOCATION="eastus"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### 1.2 Create Service Bus Namespace

```bash
# Set namespace name
SERVICEBUS_NAMESPACE="sb-assetmanager-prod"

# Create Service Bus namespace (Standard tier required for JMS)
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --location $LOCATION \
  --sku Standard
```

**Important**: 
- Standard or Premium tier is required for JMS support
- Basic tier does NOT support JMS API

### 1.3 Create Queues

```bash
# Create file processing queue
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name file-queue

# Create backup queue
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name backup-queue
```

### 1.4 Verify Queue Creation

```bash
# List all queues
az servicebus queue list \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --output table
```

---

## Step 2: Configure Managed Identity

### 2.1 Create User-Assigned Managed Identity (for App Service)

```bash
# Set identity name
IDENTITY_NAME="id-assetmanager-prod"

# Create managed identity
az identity create \
  --resource-group $RESOURCE_GROUP \
  --name $IDENTITY_NAME

# Get identity principal ID
IDENTITY_PRINCIPAL_ID=$(az identity show \
  --resource-group $RESOURCE_GROUP \
  --name $IDENTITY_NAME \
  --query principalId \
  --output tsv)

echo "Identity Principal ID: $IDENTITY_PRINCIPAL_ID"
```

### 2.2 Assign Service Bus Roles

**For Web Module (Sender):**

```bash
# Get Service Bus namespace resource ID
SERVICEBUS_ID=$(az servicebus namespace show \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --query id \
  --output tsv)

# Assign Azure Service Bus Data Sender role
az role assignment create \
  --assignee $IDENTITY_PRINCIPAL_ID \
  --role "Azure Service Bus Data Sender" \
  --scope $SERVICEBUS_ID
```

**For Worker Module (Receiver):**

```bash
# Assign Azure Service Bus Data Receiver role
az role assignment create \
  --assignee $IDENTITY_PRINCIPAL_ID \
  --role "Azure Service Bus Data Receiver" \
  --scope $SERVICEBUS_ID
```

**For applications that both send and receive:**

```bash
# Assign both roles if a single app does both operations
az role assignment create \
  --assignee $IDENTITY_PRINCIPAL_ID \
  --role "Azure Service Bus Data Owner" \
  --scope $SERVICEBUS_ID
```

### 2.3 Verify Role Assignments

```bash
az role assignment list \
  --assignee $IDENTITY_PRINCIPAL_ID \
  --scope $SERVICEBUS_ID \
  --output table
```

---

## Step 3: Configure Environment Variables

### 3.1 Required Environment Variables

Set the following environment variable for both web and worker applications:

```bash
export SERVICEBUS_NAMESPACE="sb-assetmanager-prod"
```

**Important Notes:**
- Use only the namespace name, NOT the full URL
- Do NOT include `.servicebus.windows.net`
- Example: `SERVICEBUS_NAMESPACE=sb-assetmanager-prod` ✅
- Example: `SERVICEBUS_NAMESPACE=sb-assetmanager-prod.servicebus.windows.net` ❌

### 3.2 Optional Storage Configuration

If using Azure Blob Storage (for future migration):

```bash
export AZURE_STORAGE_ACCOUNT_NAME="stassetmanagerprod"
export AZURE_STORAGE_CONTAINER_NAME="uploads"
```

---

## Step 4: Build and Test Locally

### 4.1 Build the Application

```bash
# Navigate to project root
cd /path/to/asset-manager-showpune

# Clean and build
./mvnw clean package

# Verify build
ls -lh web/target/*.jar
ls -lh worker/target/*.jar
```

### 4.2 Run Local Tests

```bash
# Run all tests
./mvnw test

# Run specific module tests
./mvnw test -pl web
./mvnw test -pl worker
```

### 4.3 Local Testing with Azure Service Bus (Optional)

For local development with Azure Service Bus:

1. Install Azure CLI and login:
   ```bash
   az login
   ```

2. Set environment variable:
   ```bash
   export SERVICEBUS_NAMESPACE="your-namespace-name"
   ```

3. Run application:
   ```bash
   # Run web module
   cd web
   ../mvnw spring-boot:run
   
   # Run worker module (in separate terminal)
   cd worker
   ../mvnw spring-boot:run
   ```

DefaultAzureCredential will automatically use your Azure CLI credentials for local authentication.

---

## Step 5: Deploy to Azure App Service

### 5.1 Create App Service Plan

```bash
# Create Linux App Service Plan
APP_SERVICE_PLAN="plan-assetmanager-prod"

az appservice plan create \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_PLAN \
  --location $LOCATION \
  --is-linux \
  --sku B2
```

### 5.2 Deploy Web Module

```bash
# Create web app
WEB_APP_NAME="app-assetmanager-web-prod"

az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan $APP_SERVICE_PLAN \
  --name $WEB_APP_NAME \
  --runtime "JAVA:17-java17"

# Assign managed identity
az webapp identity assign \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --identities $IDENTITY_NAME

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --settings SERVICEBUS_NAMESPACE=$SERVICEBUS_NAMESPACE

# Deploy JAR file
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --src-path web/target/web-0.0.1-SNAPSHOT.jar \
  --type jar
```

### 5.3 Deploy Worker Module

```bash
# Create worker app
WORKER_APP_NAME="app-assetmanager-worker-prod"

az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan $APP_SERVICE_PLAN \
  --name $WORKER_APP_NAME \
  --runtime "JAVA:17-java17"

# Assign managed identity
az webapp identity assign \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --identities $IDENTITY_NAME

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --settings SERVICEBUS_NAMESPACE=$SERVICEBUS_NAMESPACE

# Deploy JAR file
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --src-path worker/target/worker-0.0.1-SNAPSHOT.jar \
  --type jar
```

---

## Step 6: Deploy to Azure Container Apps (Alternative)

If you prefer containerized deployment:

### 6.1 Create Container Registry

```bash
ACR_NAME="acrassetmanagerprod"

az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $ACR_NAME \
  --sku Basic \
  --admin-enabled true
```

### 6.2 Build and Push Docker Images

```bash
# Login to ACR
az acr login --name $ACR_NAME

# Build and push web image
cd web
docker build -t $ACR_NAME.azurecr.io/assetmanager-web:latest .
docker push $ACR_NAME.azurecr.io/assetmanager-web:latest

# Build and push worker image
cd ../worker
docker build -t $ACR_NAME.azurecr.io/assetmanager-worker:latest .
docker push $ACR_NAME.azurecr.io/assetmanager-worker:latest
```

### 6.3 Create Container Apps Environment

```bash
CONTAINERAPPS_ENV="env-assetmanager-prod"

az containerapp env create \
  --resource-group $RESOURCE_GROUP \
  --name $CONTAINERAPPS_ENV \
  --location $LOCATION
```

### 6.4 Deploy Container Apps

```bash
# Deploy web container
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --name "ca-assetmanager-web" \
  --environment $CONTAINERAPPS_ENV \
  --image "$ACR_NAME.azurecr.io/assetmanager-web:latest" \
  --target-port 8080 \
  --ingress external \
  --registry-server "$ACR_NAME.azurecr.io" \
  --env-vars SERVICEBUS_NAMESPACE=$SERVICEBUS_NAMESPACE

# Deploy worker container
az containerapp create \
  --resource-group $RESOURCE_GROUP \
  --name "ca-assetmanager-worker" \
  --environment $CONTAINERAPPS_ENV \
  --image "$ACR_NAME.azurecr.io/assetmanager-worker:latest" \
  --registry-server "$ACR_NAME.azurecr.io" \
  --env-vars SERVICEBUS_NAMESPACE=$SERVICEBUS_NAMESPACE
```

---

## Step 7: Verification and Testing

### 7.1 Check Application Health

```bash
# Check web app logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME

# Check worker app logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME
```

### 7.2 Test Message Flow

1. **Upload a file** via web API
2. **Verify message sent** to `file-queue`
3. **Verify worker processes** the message
4. **Check Service Bus metrics** in Azure Portal

### 7.3 Monitor Service Bus

```bash
# Get queue metrics
az servicebus queue show \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name file-queue \
  --query "{ActiveMessages:messageCount, DeadLetterMessages:deadLetterMessageCount}"
```

---

## Troubleshooting

### Common Issues

#### 1. Authentication Failures

**Error**: `ManagedIdentityCredential authentication failed`

**Solutions**:
- Verify managed identity is assigned to the app
- Verify RBAC roles are correctly assigned
- Check that identity has propagated (can take a few minutes)
- Verify `SERVICEBUS_NAMESPACE` environment variable is set correctly

```bash
# Verify identity assignment
az webapp identity show \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME

# Verify role assignments
az role assignment list \
  --assignee $IDENTITY_PRINCIPAL_ID \
  --all
```

#### 2. Connection Issues

**Error**: `Could not connect to Service Bus`

**Solutions**:
- Verify Service Bus namespace exists and is Standard/Premium tier
- Check namespace name (no `.servicebus.windows.net` suffix)
- Verify queues are created
- Check network connectivity

```bash
# Verify namespace
az servicebus namespace show \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE

# Verify queues exist
az servicebus queue list \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE
```

#### 3. Message Processing Failures

**Error**: Messages stuck in queue or moved to dead-letter queue

**Solutions**:
- Check worker application logs for exceptions
- Verify message format is correct
- Check dead-letter queue for failed messages
- Review retry configuration in ServiceBusConfig

```bash
# Peek dead-letter messages
az servicebus queue message peek \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --queue-name file-queue \
  --dead-letter
```

### Debug Commands

```bash
# Enable application logging
az webapp log config \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --application-logging filesystem \
  --level verbose

# Stream logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME

# Check application configuration
az webapp config appsettings list \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --output table
```

---

## Security Best Practices

1. ✅ **Use Managed Identity**: Never store connection strings in code or config files
2. ✅ **Principle of Least Privilege**: Assign only required RBAC roles (Sender/Receiver, not Owner unless necessary)
3. ✅ **Enable Diagnostic Logs**: Configure Azure Monitor for Service Bus
4. ✅ **Use Private Endpoints**: For production, consider using Private Link for Service Bus
5. ✅ **Secure Secrets**: Use Azure Key Vault for any additional secrets
6. ✅ **Network Security**: Use Virtual Networks and NSGs to restrict traffic

---

## Rollback Procedure

If issues arise after deployment:

1. **Revert to previous deployment**:
   ```bash
   az webapp deployment list-publishing-credentials \
     --resource-group $RESOURCE_GROUP \
     --name $WEB_APP_NAME
   ```

2. **Check deployment history**:
   ```bash
   az webapp deployment list \
     --resource-group $RESOURCE_GROUP \
     --name $WEB_APP_NAME
   ```

3. **Restore from backup** if needed

---

## Performance Tuning

### Service Bus Configuration

- **Message TTL**: Configure time-to-live for messages
- **Max Delivery Count**: Adjust before moving to dead-letter queue (default: 10)
- **Lock Duration**: Adjust message lock timeout (default: 60 seconds)
- **Prefetch Count**: Enable prefetching for better throughput

### Application Scaling

```bash
# Scale web app
az webapp scale \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --instance-count 3

# Scale worker app
az webapp scale \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --instance-count 2
```

---

## Cost Optimization

- Use **Standard tier** Service Bus for JMS support (Basic doesn't support JMS)
- Consider **Premium tier** for higher throughput and larger message sizes
- Right-size App Service Plan based on actual load
- Use **autoscaling** to handle variable workloads
- Monitor costs with Azure Cost Management

---

## Support and Resources

### Documentation
- [Azure Service Bus JMS Support](https://docs.microsoft.com/azure/service-bus-messaging/service-bus-java-how-to-use-jms-api-amqp)
- [Managed Identity Overview](https://docs.microsoft.com/azure/active-directory/managed-identities-azure-resources/)
- [Spring Cloud Azure Documentation](https://docs.microsoft.com/azure/developer/java/spring-framework/)

### Monitoring
- Azure Portal: Service Bus Metrics
- Application Insights: Application performance monitoring
- Log Analytics: Centralized logging

---

## Summary

This guide covered:
- ✅ Azure Service Bus namespace and queue creation
- ✅ Managed identity configuration and RBAC role assignments
- ✅ Environment variable configuration
- ✅ Local build and testing procedures
- ✅ Deployment to Azure App Service
- ✅ Alternative deployment to Azure Container Apps
- ✅ Verification and troubleshooting steps
- ✅ Security best practices

Your Asset Manager Kit application is now ready for production deployment on Azure with modern, cloud-native messaging infrastructure!
