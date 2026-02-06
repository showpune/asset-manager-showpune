# Azure Service Bus Migration Guide

This guide provides detailed instructions for understanding and deploying the migrated Asset Manager Kit application with Azure Service Bus.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Azure Resource Setup](#azure-resource-setup)
4. [Configuration](#configuration)
5. [Local Development](#local-development)
6. [Deployment](#deployment)
7. [Verification](#verification)
8. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Before Migration (RabbitMQ)

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Web Module │ ──────> │   RabbitMQ   │ ──────> │   Worker    │
│             │  AMQP   │    Queue     │  AMQP   │   Module    │
└─────────────┘         └──────────────┘         └─────────────┘
```

### After Migration (Azure Service Bus)

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Web Module │ ──────> │Azure Service │ ──────> │   Worker    │
│             │   JMS   │  Bus Queue   │   JMS   │   Module    │
│             │         │              │         │             │
│  JmsTemplate│         │image-        │         │ @JmsListener│
└─────────────┘         │processing    │         └─────────────┘
                        └──────────────┘
                               │
                               │ Managed Identity
                               ↓
                        ┌──────────────┐
                        │ Azure RBAC   │
                        └──────────────┘
```

### Key Changes

1. **Messaging Protocol**: AMQP (RabbitMQ) → JMS over AMQP (Azure Service Bus)
2. **Authentication**: Username/Password → Managed Identity (passwordless)
3. **Dependencies**: `spring-boot-starter-amqp` → `spring-cloud-azure-starter-servicebus-jms`
4. **API**: `RabbitTemplate`/`@RabbitListener` → `JmsTemplate`/`@JmsListener`

---

## Prerequisites

### Required Software

- Java 17 or higher (JDK 17 LTS recommended)
- Maven 3.x
- Azure CLI (for resource provisioning)
- Azure subscription with appropriate permissions

### Required Azure Permissions

- **Contributor** role on the subscription or resource group (to create resources)
- Ability to assign RBAC roles to managed identities

---

## Azure Resource Setup

### Step 1: Create Resource Group

```bash
# Set variables
RESOURCE_GROUP="asset-manager-rg"
LOCATION="eastus"

# Create resource group
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### Step 2: Create Azure Service Bus Namespace

```bash
# Set variables
SERVICEBUS_NAMESPACE="asset-manager-sb-$(date +%s)"  # Unique name
SKU="Standard"  # or "Premium" for production

# Create Service Bus namespace
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --location $LOCATION \
  --sku $SKU
```

**Note**: The namespace name must be globally unique across Azure. The example above appends a timestamp to ensure uniqueness.

### Step 3: Create Service Bus Queue

```bash
# Create the queue
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name image-processing \
  --max-size 1024 \
  --default-message-time-to-live P7D
```

**Queue Settings Explained**:
- `max-size`: Maximum queue size in MB (1024 MB = 1 GB)
- `default-message-time-to-live`: Messages expire after 7 days if not processed

### Step 4: Configure Managed Identity (for Azure deployment)

If deploying to Azure App Service, Container Apps, or AKS:

```bash
# Example for Azure App Service
APP_SERVICE_NAME="asset-manager-web"

# Enable system-assigned managed identity
az webapp identity assign \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_NAME
```

### Step 5: Assign RBAC Roles

```bash
# Get the managed identity principal ID
PRINCIPAL_ID=$(az webapp identity show \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_NAME \
  --query principalId -o tsv)

# Get the Service Bus namespace resource ID
SERVICEBUS_ID=$(az servicebus namespace show \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --query id -o tsv)

# Assign roles for web module (sender)
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Sender" \
  --scope $SERVICEBUS_ID

# Assign roles for worker module (receiver)
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Receiver" \
  --scope $SERVICEBUS_ID
```

**Repeat Step 4-5** for the worker module deployment.

---

## Configuration

### Environment Variables

#### Web Module Configuration

```bash
# Azure Service Bus
SERVICEBUS_NAMESPACE=asset-manager-sb-1234567890  # Your namespace name (without .servicebus.windows.net)

# Database (existing configuration)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-server.postgres.database.azure.com:5432/assetdb
SPRING_DATASOURCE_USERNAME=dbadmin@your-db-server
SPRING_DATASOURCE_PASSWORD=YourSecurePassword

# AWS S3 (if not migrated to Azure Storage)
AWS_S3_BUCKET=your-s3-bucket
AWS_REGION=us-east-1
```

#### Worker Module Configuration

```bash
# Azure Service Bus
SERVICEBUS_NAMESPACE=asset-manager-sb-1234567890  # Same as web module

# Database (if worker needs database access)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-server.postgres.database.azure.com:5432/assetdb
SPRING_DATASOURCE_USERNAME=dbadmin@your-db-server
SPRING_DATASOURCE_PASSWORD=YourSecurePassword
```

### Application Properties

The application uses Spring profiles. The relevant properties are in:
- `web/src/main/resources/application.properties`
- `worker/src/main/resources/application.properties`

**Azure Service Bus Connection** is configured via environment variable:
```properties
# The Spring Cloud Azure library automatically uses:
# - SERVICEBUS_NAMESPACE environment variable
# - DefaultAzureCredential for authentication (managed identity in Azure)
```

---

## Local Development

### Option 1: Using Azure Service Bus (Recommended)

1. **Setup Azure Service Bus** (follow steps above)

2. **Authenticate with Azure CLI**:
   ```bash
   az login
   ```

3. **Set Environment Variables**:
   ```bash
   export SERVICEBUS_NAMESPACE=your-servicebus-namespace
   ```

4. **Run the Application**:
   ```bash
   # Start web module
   cd web
   mvn spring-boot:run

   # Start worker module (in another terminal)
   cd worker
   mvn spring-boot:run
   ```

### Option 2: Using Dev Profile (Local Filesystem)

For development without Azure Service Bus, use the `dev` profile which disables messaging:

```bash
# Set profile to dev
export SPRING_PROFILES_ACTIVE=dev

# Run the application
cd web
mvn spring-boot:run
```

**Note**: In dev mode, the worker module won't process images asynchronously.

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific module
cd web
mvn test
```

Tests use mocked JMS components and don't require Azure resources.

---

## Deployment

### Deploy to Azure App Service

#### Web Module Deployment

```bash
# Build the application
cd web
mvn clean package

# Create App Service plan (if not exists)
az appservice plan create \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-plan \
  --sku B1 \
  --is-linux

# Create web app
az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan asset-manager-plan \
  --name asset-manager-web \
  --runtime "JAVA:17-java17"

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-web \
  --settings \
    SERVICEBUS_NAMESPACE=$SERVICEBUS_NAMESPACE \
    SPRING_DATASOURCE_URL=$DB_URL \
    SPRING_DATASOURCE_USERNAME=$DB_USER \
    SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
    AWS_S3_BUCKET=$S3_BUCKET

# Deploy JAR
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-web \
  --src-path target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar
```

#### Worker Module Deployment

```bash
# Build the application
cd worker
mvn clean package

# Create web app for worker
az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan asset-manager-plan \
  --name asset-manager-worker \
  --runtime "JAVA:17-java17"

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-worker \
  --settings \
    SERVICEBUS_NAMESPACE=$SERVICEBUS_NAMESPACE \
    SPRING_DATASOURCE_URL=$DB_URL \
    SPRING_DATASOURCE_USERNAME=$DB_USER \
    SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD

# Deploy JAR
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-worker \
  --src-path target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar
```

---

## Verification

### 1. Check Service Bus Queue

```bash
# Get queue details
az servicebus queue show \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name image-processing

# Check message count
az servicebus queue show \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name image-processing \
  --query "countDetails.activeMessageCount"
```

### 2. Monitor Application Logs

```bash
# Web module logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-web

# Worker module logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name asset-manager-worker
```

### 3. Test Message Flow

1. Upload an image through the web interface
2. Check Service Bus queue for new message
3. Verify worker module processes the message
4. Check database for image metadata

---

## Troubleshooting

### Issue: "Unauthorized" or "Authentication failed"

**Cause**: Managed identity not configured or RBAC roles not assigned

**Solution**:
1. Verify managed identity is enabled:
   ```bash
   az webapp identity show --resource-group $RESOURCE_GROUP --name $APP_SERVICE_NAME
   ```
2. Verify RBAC role assignments:
   ```bash
   az role assignment list --assignee $PRINCIPAL_ID --scope $SERVICEBUS_ID
   ```
3. Wait 5-10 minutes after role assignment for propagation

### Issue: "Service Bus namespace not found"

**Cause**: Environment variable `SERVICEBUS_NAMESPACE` not set or incorrect

**Solution**:
1. Check environment variable:
   ```bash
   az webapp config appsettings list --resource-group $RESOURCE_GROUP --name $APP_SERVICE_NAME
   ```
2. Ensure value is the namespace name only (not the full URL):
   - ✅ Correct: `asset-manager-sb-123456`
   - ❌ Incorrect: `asset-manager-sb-123456.servicebus.windows.net`

### Issue: Messages not being processed

**Cause**: Worker module not receiving messages or listener not configured

**Solution**:
1. Check if worker module is running:
   ```bash
   az webapp show --resource-group $RESOURCE_GROUP --name asset-manager-worker
   ```
2. Verify worker has receiver role:
   ```bash
   az role assignment list --assignee $WORKER_PRINCIPAL_ID --scope $SERVICEBUS_ID
   ```
3. Check worker logs for errors

### Issue: Local development authentication fails

**Cause**: Azure CLI not authenticated or credentials expired

**Solution**:
1. Login to Azure CLI:
   ```bash
   az login
   az account show  # Verify correct subscription
   ```
2. Set correct subscription:
   ```bash
   az account set --subscription "Your Subscription Name"
   ```

### Issue: Messages expire before processing

**Cause**: Default message TTL too short or worker not processing fast enough

**Solution**:
1. Increase message TTL:
   ```bash
   az servicebus queue update \
     --resource-group $RESOURCE_GROUP \
     --namespace-name $SERVICEBUS_NAMESPACE \
     --name image-processing \
     --default-message-time-to-live P14D  # 14 days
   ```
2. Scale worker module (increase instance count)

---

## Additional Resources

- [Azure Service Bus Documentation](https://learn.microsoft.com/en-us/azure/service-bus-messaging/)
- [Spring Cloud Azure Service Bus JMS](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-service-bus)
- [Azure Managed Identities](https://learn.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/overview)
- [Spring Boot 3 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)

---

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section above
2. Review Azure Service Bus logs in Azure Portal
3. Check application logs using `az webapp log tail`
4. Consult the repository's issue tracker

---

**Last Updated**: 2026-02-06  
**Version**: 1.0
