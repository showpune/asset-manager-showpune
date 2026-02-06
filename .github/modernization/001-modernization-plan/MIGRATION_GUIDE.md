# Azure Migration Guide

This guide provides step-by-step instructions for deploying the Asset Manager Kit application to Azure after completing the modernization plan.

---

## Prerequisites

- Azure subscription
- Azure CLI installed and configured
- Maven 3.x and Java 17 installed locally
- Git access to the repository

---

## Architecture Overview

The modernized application consists of two modules:

1. **Web Module** - Handles file uploads and sends messages to Azure Service Bus
2. **Worker Module** - Consumes messages from Azure Service Bus and processes images

Both modules connect to:
- Azure Service Bus (for async messaging)
- PostgreSQL database (for metadata)
- Storage backend (AWS S3, Azure Blob Storage, or local filesystem)

---

## Step 1: Create Azure Resources

### 1.1 Create Azure Service Bus Namespace

```bash
# Set variables
RESOURCE_GROUP="rg-asset-manager"
LOCATION="eastus"
SERVICEBUS_NAMESPACE="sb-asset-manager-${RANDOM}"

# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create Service Bus namespace (Premium tier required for JMS support)
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
  --location $LOCATION \
  --sku Premium

# Create queue
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $SERVICEBUS_NAMESPACE \
  --name image-processing
```

### 1.2 Create Azure Database for PostgreSQL

```bash
# Set variables
POSTGRES_SERVER="psql-asset-manager-${RANDOM}"
POSTGRES_ADMIN_USER="assetadmin"
POSTGRES_ADMIN_PASSWORD="<strong-password>"
POSTGRES_DATABASE="assets_manager"

# Create PostgreSQL server
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --name $POSTGRES_SERVER \
  --location $LOCATION \
  --admin-user $POSTGRES_ADMIN_USER \
  --admin-password $POSTGRES_ADMIN_PASSWORD \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --version 14

# Create database
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name $POSTGRES_SERVER \
  --database-name $POSTGRES_DATABASE

# Configure firewall to allow Azure services
az postgres flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name $POSTGRES_SERVER \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0
```

---

## Step 2: Deploy Applications

### 2.1 Create App Service Plans and Web Apps

```bash
# Create App Service Plan for web module
az appservice plan create \
  --resource-group $RESOURCE_GROUP \
  --name plan-asset-manager-web \
  --location $LOCATION \
  --sku B1 \
  --is-linux

# Create App Service Plan for worker module
az appservice plan create \
  --resource-group $RESOURCE_GROUP \
  --name plan-asset-manager-worker \
  --location $LOCATION \
  --sku B1 \
  --is-linux

# Create web app for web module
WEB_APP_NAME="app-asset-manager-web-${RANDOM}"
az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan plan-asset-manager-web \
  --name $WEB_APP_NAME \
  --runtime "JAVA:17-java17"

# Create web app for worker module
WORKER_APP_NAME="app-asset-manager-worker-${RANDOM}"
az webapp create \
  --resource-group $RESOURCE_GROUP \
  --plan plan-asset-manager-worker \
  --name $WORKER_APP_NAME \
  --runtime "JAVA:17-java17"
```

### 2.2 Enable Managed Identity

```bash
# Enable system-assigned managed identity for web app
az webapp identity assign \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME

# Enable system-assigned managed identity for worker app
az webapp identity assign \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME

# Get the principal IDs (note these for next step)
WEB_PRINCIPAL_ID=$(az webapp identity show \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --query principalId -o tsv)

WORKER_PRINCIPAL_ID=$(az webapp identity show \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --query principalId -o tsv)
```

---

## Step 3: Configure RBAC Roles

### 3.1 Assign Service Bus Roles

```bash
# Get Service Bus namespace resource ID
SERVICEBUS_ID=$(az servicebus namespace show \
  --resource-group $RESOURCE_GROUP \
  --name $SERVICEBUS_NAMESPACE \
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

---

## Step 4: Configure Application Settings

### 4.1 Configure Web Application

```bash
# Set Service Bus configuration
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --settings \
    SERVICE_BUS_NAMESPACE="$SERVICEBUS_NAMESPACE" \
    spring.cloud.azure.credential.managed-identity-enabled="true" \
    spring.cloud.azure.servicebus.entity-type="queue" \
    spring.jms.servicebus.pricing-tier="premium"

# Set database configuration
POSTGRES_CONN_STRING="jdbc:postgresql://${POSTGRES_SERVER}.postgres.database.azure.com:5432/${POSTGRES_DATABASE}?sslmode=require"

az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --settings \
    spring.datasource.url="$POSTGRES_CONN_STRING" \
    spring.datasource.username="$POSTGRES_ADMIN_USER" \
    spring.datasource.password="$POSTGRES_ADMIN_PASSWORD"

# Set storage configuration (if using AWS S3)
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --settings \
    aws.accessKeyId="<your-aws-access-key>" \
    aws.secretKey="<your-aws-secret-key>" \
    aws.region="us-east-1" \
    aws.s3.bucket="<your-bucket-name>"
```

### 4.2 Configure Worker Application

```bash
# Set Service Bus configuration
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --settings \
    SERVICE_BUS_NAMESPACE="$SERVICEBUS_NAMESPACE" \
    spring.cloud.azure.credential.managed-identity-enabled="true" \
    spring.cloud.azure.servicebus.entity-type="queue" \
    spring.jms.servicebus.pricing-tier="premium"

# Set database configuration
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --settings \
    spring.datasource.url="$POSTGRES_CONN_STRING" \
    spring.datasource.username="$POSTGRES_ADMIN_USER" \
    spring.datasource.password="$POSTGRES_ADMIN_PASSWORD"

# Set storage configuration (if using AWS S3)
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --settings \
    aws.accessKeyId="<your-aws-access-key>" \
    aws.secretKey="<your-aws-secret-key>" \
    aws.region="us-east-1" \
    aws.s3.bucket="<your-bucket-name>"
```

---

## Step 5: Build and Deploy

### 5.1 Build the Application

```bash
# Build both modules
./mvnw clean package -DskipTests

# The JAR files will be in:
# web/target/assets-manager-web-0.0.1-SNAPSHOT.jar
# worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar
```

### 5.2 Deploy to Azure

```bash
# Deploy web module
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --src-path web/target/assets-manager-web-0.0.1-SNAPSHOT.jar \
  --type jar

# Deploy worker module
az webapp deploy \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --src-path worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar \
  --type jar
```

---

## Step 6: Verify Deployment

### 6.1 Check Application Logs

```bash
# View web app logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME

# View worker app logs
az webapp log tail \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME
```

### 6.2 Test the Application

1. Get the web app URL:
```bash
WEB_APP_URL=$(az webapp show \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --query defaultHostName -o tsv)

echo "Web App URL: https://$WEB_APP_URL"
```

2. Open the URL in a browser
3. Upload an image file
4. Verify the thumbnail is generated
5. Check Service Bus queue metrics in Azure portal

---

## Alternative: Using Azure Blob Storage

If you prefer to use Azure Blob Storage instead of AWS S3:

### Create Storage Account

```bash
STORAGE_ACCOUNT="stassetmgr${RANDOM}"

# Create storage account
az storage account create \
  --resource-group $RESOURCE_GROUP \
  --name $STORAGE_ACCOUNT \
  --location $LOCATION \
  --sku Standard_LRS

# Create container
az storage container create \
  --account-name $STORAGE_ACCOUNT \
  --name images \
  --auth-mode login
```

### Assign Storage Roles

```bash
STORAGE_ID=$(az storage account show \
  --resource-group $RESOURCE_GROUP \
  --name $STORAGE_ACCOUNT \
  --query id -o tsv)

# Assign Storage Blob Data Contributor to both apps
az role assignment create \
  --assignee $WEB_PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID

az role assignment create \
  --assignee $WORKER_PRINCIPAL_ID \
  --role "Storage Blob Data Contributor" \
  --scope $STORAGE_ID
```

### Update Application Settings

```bash
# Update web app to use Azure storage
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --settings \
    SPRING_PROFILES_ACTIVE="azure" \
    azure.storage.account-name="$STORAGE_ACCOUNT" \
    azure.storage.container-name="images"

# Update worker app to use Azure storage
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --settings \
    SPRING_PROFILES_ACTIVE="azure" \
    azure.storage.account-name="$STORAGE_ACCOUNT" \
    azure.storage.container-name="images"
```

---

## Troubleshooting

### Issue: Application fails to connect to Service Bus

**Solution:**
1. Verify managed identity is enabled
2. Check RBAC role assignments
3. Ensure SERVICE_BUS_NAMESPACE environment variable is set correctly (name only, not full URL)
4. Verify Service Bus namespace is Premium tier (Standard tier doesn't support JMS)

### Issue: Database connection fails

**Solution:**
1. Check firewall rules allow Azure services
2. Verify connection string format includes `?sslmode=require`
3. Ensure credentials are correct

### Issue: Messages not being processed

**Solution:**
1. Check worker app logs for errors
2. Verify queue name is "image-processing"
3. Check Service Bus queue metrics in Azure portal
4. Ensure worker app has Data Receiver role

---

## Monitoring and Observability

### Enable Application Insights

```bash
# Create Application Insights
az monitor app-insights component create \
  --resource-group $RESOURCE_GROUP \
  --app asset-manager-insights \
  --location $LOCATION \
  --application-type web

# Get instrumentation key
INSIGHTS_KEY=$(az monitor app-insights component show \
  --resource-group $RESOURCE_GROUP \
  --app asset-manager-insights \
  --query instrumentationKey -o tsv)

# Configure both apps to use Application Insights
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WEB_APP_NAME \
  --settings APPINSIGHTS_INSTRUMENTATIONKEY="$INSIGHTS_KEY"

az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $WORKER_APP_NAME \
  --settings APPINSIGHTS_INSTRUMENTATIONKEY="$INSIGHTS_KEY"
```

---

## Cost Optimization Tips

1. **Use appropriate Service Bus tier:**
   - Development/Testing: Use Premium tier with minimal messaging units
   - Production: Scale messaging units based on load

2. **Right-size App Service plans:**
   - Start with B1 (Basic) tier
   - Monitor CPU/memory usage and scale as needed

3. **Use Azure Database for PostgreSQL Flexible Server:**
   - Burstable tier for development
   - General Purpose for production

4. **Enable auto-pause for non-production environments:**
   ```bash
   az webapp stop --resource-group $RESOURCE_GROUP --name $WEB_APP_NAME
   ```

---

## Security Best Practices

1. **Always use managed identities** - Avoid storing credentials in configuration
2. **Enable SSL/TLS** - Ensure all connections use encryption
3. **Use Azure Key Vault** - Store sensitive configuration in Key Vault
4. **Implement network security** - Use Virtual Networks and Private Endpoints for production
5. **Enable diagnostic logging** - Send logs to Log Analytics workspace
6. **Regular updates** - Keep dependencies and runtime versions up to date

---

## Support

For issues or questions:
- Check application logs in Azure portal
- Review Service Bus metrics and diagnostics
- Consult Azure documentation: https://docs.microsoft.com/azure
- Open an issue in the repository
