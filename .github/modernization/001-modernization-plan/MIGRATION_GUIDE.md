# Azure Service Bus Migration Guide

**Project**: Asset Manager Kit  
**Migration**: RabbitMQ → Azure Service Bus  
**Date**: February 6, 2026

---

## Overview

This guide provides detailed instructions for deploying and operating the Asset Manager Kit application after migration to Azure Service Bus. The application has been upgraded from Java 11 / Spring Boot 2.7 to Java 17 / Spring Boot 3.2.5 and migrated from RabbitMQ to Azure Service Bus with managed identity authentication.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Resources Setup](#azure-resources-setup)
3. [Application Configuration](#application-configuration)
4. [Local Development Setup](#local-development-setup)
5. [Deployment to Azure](#deployment-to-azure)
6. [Testing](#testing)
7. [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)
8. [Rollback Strategy](#rollback-strategy)

---

## Prerequisites

### Development Environment
- **Java**: OpenJDK 17 or later (LTS recommended)
- **Maven**: 3.6 or later
- **IDE**: IntelliJ IDEA, VS Code, or Eclipse with Java 17 support
- **Azure CLI**: 2.50 or later (for Azure resource management)
- **Git**: For version control

### Azure Subscription
- Active Azure subscription
- Permissions to create and manage:
  - Azure Service Bus namespaces
  - Managed identities
  - RBAC role assignments
  - Azure App Service or Azure Container Apps (for deployment)

### Knowledge Requirements
- Basic understanding of Spring Boot applications
- Familiarity with Azure portal or Azure CLI
- Understanding of managed identity concepts

---

## Azure Resources Setup

### Step 1: Create Azure Service Bus Namespace

#### Using Azure Portal
1. Navigate to Azure Portal (https://portal.azure.com)
2. Click **Create a resource** → **Integration** → **Service Bus**
3. Configure the namespace:
   - **Subscription**: Select your subscription
   - **Resource Group**: Create new or select existing
   - **Namespace name**: `assetmanager-servicebus-[env]` (e.g., dev, test, prod)
   - **Location**: Choose appropriate region
   - **Pricing tier**: Standard (supports topics and subscriptions)
4. Click **Review + Create** → **Create**

#### Using Azure CLI
```bash
# Set variables
RESOURCE_GROUP="assetmanager-rg"
LOCATION="eastus"
NAMESPACE_NAME="assetmanager-servicebus-dev"

# Create resource group (if not exists)
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Service Bus namespace
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $NAMESPACE_NAME \
  --location $LOCATION \
  --sku Standard
```

### Step 2: Create Service Bus Queues

The application requires queues for message processing. Create the following queues:

#### Using Azure Portal
1. Navigate to your Service Bus namespace
2. Click **Queues** → **+ Queue**
3. Create queue(s):
   - **Name**: `asset-processing-queue` (or as per your application needs)
   - **Max delivery count**: 10
   - **Message time to live**: 14 days (default)
   - **Lock duration**: 30 seconds
   - **Enable sessions**: No (unless required)
   - **Enable duplicate detection**: Optional
4. Click **Create**

#### Using Azure CLI
```bash
QUEUE_NAME="asset-processing-queue"

az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name $QUEUE_NAME \
  --max-delivery-count 10 \
  --lock-duration PT30S
```

### Step 3: Configure Managed Identity

#### For Azure App Service / Azure Functions
```bash
# Enable system-assigned managed identity
APP_NAME="assetmanager-web-dev"
az webapp identity assign --name $APP_NAME --resource-group $RESOURCE_GROUP

# Get the managed identity principal ID
PRINCIPAL_ID=$(az webapp identity show --name $APP_NAME --resource-group $RESOURCE_GROUP --query principalId -o tsv)
```

#### For Azure Container Apps
```bash
CONTAINER_APP_NAME="assetmanager-web-dev"
az containerapp identity assign --name $CONTAINER_APP_NAME --resource-group $RESOURCE_GROUP

PRINCIPAL_ID=$(az containerapp identity show --name $CONTAINER_APP_NAME --resource-group $RESOURCE_GROUP --query principalId -o tsv)
```

### Step 4: Assign RBAC Roles

Assign the necessary roles to the managed identity:

```bash
# Get Service Bus namespace resource ID
SERVICEBUS_ID=$(az servicebus namespace show \
  --resource-group $RESOURCE_GROUP \
  --name $NAMESPACE_NAME \
  --query id -o tsv)

# Assign Azure Service Bus Data Sender role
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Sender" \
  --scope $SERVICEBUS_ID

# Assign Azure Service Bus Data Receiver role
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Receiver" \
  --scope $SERVICEBUS_ID
```

**Required Roles**:
- **Azure Service Bus Data Sender**: Allows sending messages to queues/topics
- **Azure Service Bus Data Receiver**: Allows receiving messages from queues/subscriptions

---

## Application Configuration

### Configuration Files

The application uses Spring profiles for different environments:
- `dev` profile: Local development with file storage
- `azure` profile: Azure deployment with Azure Service Bus

### Service Bus Configuration

Update the application configuration for Azure Service Bus:

#### For application.properties
```properties
# Azure Service Bus Configuration
spring.jms.servicebus.connection-string=${SERVICEBUS_CONNECTION_STRING}
spring.jms.servicebus.namespace=${SERVICEBUS_NAMESPACE}
spring.jms.servicebus.pricing-tier=standard

# Queue names
app.servicebus.queue.asset-processing=asset-processing-queue
```

#### For application.yml
```yaml
spring:
  jms:
    servicebus:
      namespace: ${SERVICEBUS_NAMESPACE}
      pricing-tier: standard
      # Use managed identity (no connection string needed in production)

app:
  servicebus:
    queue:
      asset-processing: asset-processing-queue
```

### Environment Variables

Set the following environment variables:

#### For Local Development (using connection string)
```bash
export SERVICEBUS_NAMESPACE="assetmanager-servicebus-dev.servicebus.windows.net"
export SERVICEBUS_CONNECTION_STRING="Endpoint=sb://assetmanager-servicebus-dev.servicebus.windows.net/;..."
```

#### For Azure Deployment (using managed identity)
```bash
# Only namespace needed - managed identity handles authentication
export SERVICEBUS_NAMESPACE="assetmanager-servicebus-dev.servicebus.windows.net"
```

### Getting Connection String (for development)

```bash
az servicebus namespace authorization-rule keys list \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString -o tsv
```

**⚠️ Security Note**: Connection strings should only be used for local development. Always use managed identity in Azure environments.

---

## Local Development Setup

### Step 1: Clone and Build

```bash
# Clone repository
git clone <repository-url>
cd asset-manager-showpune

# Build the project
mvn clean install
```

### Step 2: Configure Local Environment

Create a `.env` file or set environment variables:

```bash
# .env file (for local development)
SERVICEBUS_NAMESPACE=assetmanager-servicebus-dev.servicebus.windows.net
SERVICEBUS_CONNECTION_STRING=Endpoint=sb://...
```

### Step 3: Run Web Application

```bash
cd web
mvn spring-boot:run
```

The web application will start on `http://localhost:8080`

### Step 4: Run Worker Application

```bash
cd worker
mvn spring-boot:run
```

The worker application will start and listen for messages from Service Bus.

### Step 5: Test Message Flow

1. Access the web application at `http://localhost:8080`
2. Upload an image file
3. The web application sends a message to Azure Service Bus
4. The worker application receives the message and processes the image
5. Check logs for message processing confirmation

---

## Deployment to Azure

### Option 1: Azure App Service (JAR Deployment)

#### Deploy Web Application
```bash
# Package the application
cd web
mvn clean package

# Create App Service (if not exists)
az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan assetmanager-plan \
  --name assetmanager-web-dev \
  --runtime "JAVA:17-java17"

# Deploy JAR
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name assetmanager-web-dev \
  --src-path target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name assetmanager-web-dev \
  --settings \
    SERVICEBUS_NAMESPACE="assetmanager-servicebus-dev.servicebus.windows.net" \
    SPRING_PROFILES_ACTIVE="azure"
```

#### Deploy Worker Application
```bash
cd worker
mvn clean package

az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan assetmanager-plan \
  --name assetmanager-worker-dev \
  --runtime "JAVA:17-java17"

az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name assetmanager-worker-dev \
  --src-path target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar

az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name assetmanager-worker-dev \
  --settings \
    SERVICEBUS_NAMESPACE="assetmanager-servicebus-dev.servicebus.windows.net" \
    SPRING_PROFILES_ACTIVE="azure"
```

### Option 2: Azure Container Apps

#### Prerequisites
- Docker installed locally
- Container registry (Azure Container Registry)

#### Build and Push Container Images

```bash
# Login to Azure Container Registry
ACR_NAME="assetmanageracr"
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

#### Deploy to Container Apps

```bash
# Create Container Apps environment
az containerapp env create \
  --name assetmanager-env \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Deploy web container app
az containerapp create \
  --name assetmanager-web-dev \
  --resource-group $RESOURCE_GROUP \
  --environment assetmanager-env \
  --image $ACR_NAME.azurecr.io/assetmanager-web:latest \
  --target-port 8080 \
  --ingress external \
  --registry-server $ACR_NAME.azurecr.io \
  --env-vars \
    SERVICEBUS_NAMESPACE="assetmanager-servicebus-dev.servicebus.windows.net" \
    SPRING_PROFILES_ACTIVE="azure"

# Deploy worker container app
az containerapp create \
  --name assetmanager-worker-dev \
  --resource-group $RESOURCE_GROUP \
  --environment assetmanager-env \
  --image $ACR_NAME.azurecr.io/assetmanager-worker:latest \
  --ingress internal \
  --registry-server $ACR_NAME.azurecr.io \
  --env-vars \
    SERVICEBUS_NAMESPACE="assetmanager-servicebus-dev.servicebus.windows.net" \
    SPRING_PROFILES_ACTIVE="azure"
```

---

## Testing

### Unit Tests

Run all unit tests:
```bash
mvn test
```

### Integration Tests

For integration tests with actual Azure Service Bus:

1. Ensure Azure Service Bus resources are provisioned
2. Configure connection details in test configuration
3. Run integration tests:
```bash
mvn verify -P integration-tests
```

### Manual Testing

1. **Upload Test**: Upload an image via web UI
2. **Check Queue**: Verify message appears in Azure Service Bus queue (Azure Portal)
3. **Worker Processing**: Verify worker processes the message
4. **Check Results**: Verify processed image/thumbnail is created

### Smoke Tests in Production

```bash
# Check web application health
curl https://assetmanager-web-dev.azurewebsites.net/actuator/health

# Check worker application health
curl https://assetmanager-worker-dev.azurewebsites.net/actuator/health
```

---

## Monitoring and Troubleshooting

### Enable Application Insights

```bash
# Create Application Insights
az monitor app-insights component create \
  --app assetmanager-insights \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP

# Get instrumentation key
INSTRUMENTATION_KEY=$(az monitor app-insights component show \
  --app assetmanager-insights \
  --resource-group $RESOURCE_GROUP \
  --query instrumentationKey -o tsv)

# Configure App Service
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name assetmanager-web-dev \
  --settings APPLICATIONINSIGHTS_CONNECTION_STRING="InstrumentationKey=$INSTRUMENTATION_KEY"
```

### Monitor Service Bus Metrics

Key metrics to monitor:
- **Active Messages**: Number of messages waiting in queue
- **Dead-letter Messages**: Messages that failed processing
- **Incoming Messages**: Rate of message publishing
- **Outgoing Messages**: Rate of message consumption
- **Server Errors**: Service Bus errors

### Common Issues and Solutions

#### Issue 1: "Unauthorized access" errors
**Solution**: Verify managed identity has correct RBAC roles assigned
```bash
# List role assignments
az role assignment list --assignee $PRINCIPAL_ID --scope $SERVICEBUS_ID
```

#### Issue 2: Messages not being processed
**Solution**: 
- Check worker application is running
- Verify queue name configuration matches actual queue
- Check worker logs for exceptions

#### Issue 3: Connection timeout
**Solution**:
- Verify Service Bus namespace is accessible from your deployment
- Check network security group rules
- Verify firewall settings on Service Bus

#### Issue 4: "Resource not found" errors
**Solution**: Verify SERVICEBUS_NAMESPACE environment variable is correct
```bash
# Test namespace connectivity
nslookup assetmanager-servicebus-dev.servicebus.windows.net
```

### Logs and Diagnostics

```bash
# View web app logs
az webapp log tail --resource-group $RESOURCE_GROUP --name assetmanager-web-dev

# View worker app logs
az webapp log tail --resource-group $RESOURCE_GROUP --name assetmanager-worker-dev

# Download logs
az webapp log download --resource-group $RESOURCE_GROUP --name assetmanager-web-dev
```

---

## Rollback Strategy

### Rolling Back Code Changes

If issues arise after deployment:

```bash
# Redeploy previous version
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name assetmanager-web-dev \
  --src-path previous-version.jar \
  --type jar
```

### Switching Back to RabbitMQ

If a full rollback to RabbitMQ is required:

1. Restore previous branch/commit with RabbitMQ configuration
2. Redeploy RabbitMQ infrastructure
3. Update application configuration to use RabbitMQ connection
4. Redeploy applications

**Note**: This should only be done in emergency situations. Plan for thorough testing before production migration.

---

## Best Practices

1. **Always use Managed Identity in Azure**: Never store connection strings in code or configuration
2. **Enable Dead-Letter Queue**: Configure for failed message handling
3. **Set up Alerts**: Monitor queue depth and processing failures
4. **Implement Retry Logic**: Handle transient failures gracefully
5. **Use Session**: Enable sessions for ordered message processing if needed
6. **Scale Workers**: Monitor queue depth and scale worker instances accordingly
7. **Test Failover**: Regularly test application behavior during Service Bus maintenance

---

## Additional Resources

- [Azure Service Bus Documentation](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Spring Cloud Azure Service Bus](https://docs.microsoft.com/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-service-bus)
- [Managed Identity Overview](https://docs.microsoft.com/azure/active-directory/managed-identities-azure-resources/)
- [Spring Boot 3 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)

---

## Support and Contacts

For issues or questions:
- **Technical Issues**: Open an issue in the repository
- **Azure Support**: Contact Azure support via Azure Portal
- **Application Issues**: Contact development team

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-06  
**Maintained By**: DevOps Team
