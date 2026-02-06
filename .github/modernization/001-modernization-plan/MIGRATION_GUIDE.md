# Migration Guide: RabbitMQ to Azure Service Bus

This guide provides step-by-step instructions for deploying the Asset Manager Kit application after migration from RabbitMQ to Azure Service Bus.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Azure Resource Setup](#azure-resource-setup)
3. [Application Configuration](#application-configuration)
4. [Deployment Steps](#deployment-steps)
5. [Verification](#verification)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Azure Resources

- Azure Subscription with appropriate permissions
- Azure Service Bus namespace
- Azure compute resources (App Service, Container Apps, or AKS)
- Managed identity enabled on compute resources

### Required Tools

- Azure CLI (version 2.30.0 or higher)
- Java 17 or higher
- Maven 3.6 or higher

---

## Azure Resource Setup

### Step 1: Create Azure Service Bus Namespace

```bash
# Set your variables
RESOURCE_GROUP="your-resource-group"
LOCATION="eastus"
SERVICEBUS_NAMESPACE="your-servicebus-namespace"

# Create resource group (if not exists)
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Service Bus namespace (Standard or Premium tier)
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --location $LOCATION \
  --sku Standard
```

### Step 2: Create Service Bus Queues

The application requires queues for message processing. Create them based on your application configuration:

```bash
# Create queue for file processing (example)
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name file-processing-queue \
  --max-size 1024 \
  --default-message-time-to-live P14D

# Add additional queues as needed based on your application
```

### Step 3: Configure Managed Identity

#### For Azure App Service or Container Apps:

```bash
# Enable system-assigned managed identity
az webapp identity assign \
  --name your-app-name \
  --resource-group $RESOURCE_GROUP

# Or for Container Apps
az containerapp identity assign \
  --name your-app-name \
  --resource-group $RESOURCE_GROUP \
  --system-assigned
```

#### For Azure Kubernetes Service (AKS):

```bash
# Enable workload identity on your AKS cluster
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name your-aks-cluster \
  --enable-workload-identity \
  --enable-oidc-issuer

# Create managed identity
az identity create \
  --name your-app-identity \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION
```

### Step 4: Assign RBAC Roles

Grant the managed identity appropriate permissions to access Service Bus:

```bash
# Get the managed identity principal ID
PRINCIPAL_ID=$(az webapp identity show \
  --name your-app-name \
  --resource-group $RESOURCE_GROUP \
  --query principalId \
  --output tsv)

# Get the Service Bus resource ID
SERVICEBUS_ID=$(az servicebus namespace show \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --query id \
  --output tsv)

# Assign Azure Service Bus Data Sender role (for web module)
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Sender" \
  --scope $SERVICEBUS_ID

# Assign Azure Service Bus Data Receiver role (for worker module)
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Receiver" \
  --scope $SERVICEBUS_ID
```

---

## Application Configuration

### Environment Variables

Set the following environment variables in your deployment configuration:

```bash
# Required: Service Bus namespace
SERVICEBUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net

# Required: Activate Azure profile
SPRING_PROFILES_ACTIVE=azure

# Optional: Queue name (if different from default)
SERVICEBUS_QUEUE_NAME=file-processing-queue

# Optional: For other Azure services (if using Azure Blob Storage)
AZURE_STORAGE_ACCOUNT_NAME=your-storage-account
```

### Application Properties

The application is configured to use Azure Service Bus when the `azure` profile is active. The configuration uses managed identity for authentication and does not require any connection strings or passwords.

**Key Configuration Points**:

1. **Managed Identity Authentication**: Uses `DefaultAzureCredential` which automatically detects and uses:
   - Managed Identity (in Azure)
   - Azure CLI credentials (local development)
   - Environment variables (if needed)

2. **Service Bus Connection**: Configured via `SERVICEBUS_NAMESPACE` environment variable

3. **JMS Configuration**: Uses Spring Cloud Azure Service Bus JMS starter for JMS 2.0 API support

---

## Deployment Steps

### Option 1: Deploy to Azure App Service

#### Build the Application

```bash
# Navigate to project root
cd asset-manager-showpune

# Build both modules
mvn clean package -DskipTests
```

#### Deploy Web Module

```bash
# Deploy web module JAR
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name your-web-app-name \
  --src-path web/target/web-0.0.1-SNAPSHOT.jar \
  --type jar

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name your-web-app-name \
  --settings \
    SERVICEBUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net \
    SPRING_PROFILES_ACTIVE=azure
```

#### Deploy Worker Module

```bash
# Deploy worker module JAR
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name your-worker-app-name \
  --src-path worker/target/worker-0.0.1-SNAPSHOT.jar \
  --type jar

# Configure environment variables
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name your-worker-app-name \
  --settings \
    SERVICEBUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net \
    SPRING_PROFILES_ACTIVE=azure
```

### Option 2: Deploy to Azure Container Apps

#### Build Docker Images

```bash
# Build web module image
docker build -t your-registry.azurecr.io/asset-manager-web:latest -f web/Dockerfile .

# Build worker module image
docker build -t your-registry.azurecr.io/asset-manager-worker:latest -f worker/Dockerfile .

# Push to Azure Container Registry
az acr login --name your-registry
docker push your-registry.azurecr.io/asset-manager-web:latest
docker push your-registry.azurecr.io/asset-manager-worker:latest
```

#### Deploy Container Apps

```bash
# Create Container Apps environment (if not exists)
az containerapp env create \
  --name your-containerapp-env \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Deploy web container app
az containerapp create \
  --name asset-manager-web \
  --resource-group $RESOURCE_GROUP \
  --environment your-containerapp-env \
  --image your-registry.azurecr.io/asset-manager-web:latest \
  --target-port 8080 \
  --ingress external \
  --registry-server your-registry.azurecr.io \
  --system-assigned \
  --env-vars \
    SERVICEBUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net \
    SPRING_PROFILES_ACTIVE=azure

# Deploy worker container app
az containerapp create \
  --name asset-manager-worker \
  --resource-group $RESOURCE_GROUP \
  --environment your-containerapp-env \
  --image your-registry.azurecr.io/asset-manager-worker:latest \
  --registry-server your-registry.azurecr.io \
  --system-assigned \
  --env-vars \
    SERVICEBUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net \
    SPRING_PROFILES_ACTIVE=azure

# Assign RBAC roles to container app identities (repeat Step 4 from above)
```

---

## Verification

### Check Application Health

```bash
# Check web app status
az webapp show \
  --resource-group $RESOURCE_GROUP \
  --name your-web-app-name \
  --query state

# Check logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name your-web-app-name
```

### Test Message Flow

1. **Upload a file through the web interface**
   - Access your web application URL
   - Upload a test file
   - Verify the file is uploaded successfully

2. **Check Service Bus Queue**
   ```bash
   # View messages in queue
   az servicebus queue show \
     --resource-group $RESOURCE_GROUP \
     --namespace-name $SERVICEBUS_NAMESPACE \
     --name file-processing-queue \
     --query countDetails
   ```

3. **Verify Worker Processing**
   - Check worker logs to confirm message consumption
   - Verify file processing completed successfully
   - Check for any error messages in logs

### Monitor with Azure Portal

1. Navigate to Azure Portal
2. Go to your Service Bus namespace
3. Monitor:
   - Message count in queues
   - Incoming and outgoing messages
   - Failed message count
4. Check Application Insights (if configured) for:
   - Application performance
   - Exceptions and errors
   - Custom telemetry

---

## Troubleshooting

### Common Issues

#### 1. Authentication Failures

**Symptom**: `401 Unauthorized` or authentication errors in logs

**Solution**:
- Verify managed identity is enabled on your compute resource
- Check RBAC role assignments are correct
- Ensure roles are assigned at the namespace level, not individual queues
- Wait a few minutes for role assignments to propagate

```bash
# Verify role assignments
az role assignment list \
  --assignee $PRINCIPAL_ID \
  --scope $SERVICEBUS_ID
```

#### 2. Connection Errors

**Symptom**: `UnknownHostException` or connection timeout errors

**Solution**:
- Verify `SERVICEBUS_NAMESPACE` environment variable is set correctly
- Ensure format is `<namespace>.servicebus.windows.net`
- Check network connectivity from your compute resource to Service Bus
- Verify Service Bus namespace is in the same Azure region or has global access

#### 3. Queue Not Found

**Symptom**: `MessagingEntityNotFoundException` or "queue does not exist" errors

**Solution**:
- Verify queue names match between application configuration and Azure
- Check queues exist in the Service Bus namespace
- Ensure queue names are case-sensitive matches

```bash
# List all queues
az servicebus queue list \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --query "[].name"
```

#### 4. Messages Not Being Processed

**Symptom**: Messages accumulate in queue, worker doesn't process them

**Solution**:
- Check worker application is running
- Verify worker has correct RBAC roles (Data Receiver)
- Check worker logs for exceptions
- Ensure worker's `@JmsListener` configuration matches queue name

#### 5. Managed Identity Not Working Locally

**Symptom**: Application works in Azure but fails locally

**Solution**:
For local development, use Azure CLI authentication:

```bash
# Login with Azure CLI
az login

# Or set environment variables for service principal (not recommended for production)
export AZURE_CLIENT_ID=your-client-id
export AZURE_CLIENT_SECRET=your-client-secret
export AZURE_TENANT_ID=your-tenant-id
```

### Enable Debug Logging

Add to application properties for troubleshooting:

```yaml
logging:
  level:
    com.azure.messaging: DEBUG
    com.azure.identity: DEBUG
    org.springframework.jms: DEBUG
```

### Viewing Logs

#### App Service Logs
```bash
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name your-app-name
```

#### Container Apps Logs
```bash
az containerapp logs show \
  --name your-container-app \
  --resource-group $RESOURCE_GROUP \
  --follow
```

---

## Performance Tuning

### Service Bus Configuration

For high-throughput scenarios:

1. **Use Premium Tier**: Provides dedicated resources and higher throughput
2. **Enable Partitioning**: Improves availability and throughput
3. **Adjust Message TTL**: Based on your business requirements
4. **Configure Dead Letter Queue**: For failed message handling

### Application Configuration

```yaml
spring:
  jms:
    listener:
      concurrency: 5-10  # Adjust based on load
    cache:
      enabled: true
```

---

## Security Best Practices

1. ✅ **Use Managed Identity**: Never store connection strings or keys in code
2. ✅ **Principle of Least Privilege**: Assign only required RBAC roles
3. ✅ **Network Security**: Use Private Endpoints for Service Bus in production
4. ✅ **Monitor Access**: Enable diagnostic logs for security auditing
5. ✅ **Rotate Credentials**: Not needed with managed identity, but rotate service principal credentials if used

---

## Additional Resources

- [Azure Service Bus Documentation](https://learn.microsoft.com/en-us/azure/service-bus-messaging/)
- [Spring Cloud Azure Service Bus](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-service-bus)
- [Managed Identity Documentation](https://learn.microsoft.com/en-us/azure/active-directory/managed-identities-azure-resources/)
- [Azure RBAC Roles](https://learn.microsoft.com/en-us/azure/role-based-access-control/built-in-roles)

---

## Support

For issues or questions:
1. Check application logs first
2. Review Service Bus metrics in Azure Portal
3. Consult this migration guide
4. Refer to Azure Service Bus documentation
5. Contact your Azure support team

---

**Last Updated**: February 6, 2026  
**Migration Plan**: 001-modernization-plan  
**Version**: 1.0
