# Migration from AWS S3 to Azure Storage Account

This document describes the migration of the Asset Manager application from AWS S3 to Azure Storage Account.

## Overview

The application has been successfully migrated from AWS S3 to Azure Storage Account. The migration maintains the existing functionality while replacing AWS-specific implementations with Azure Storage equivalents.

## Changes Made

### 1. Dependencies Updated

**Before (AWS S3):**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.13</version>
</dependency>
```

**After (Azure Storage):**
```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.29.0</version>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.15.4</version>
</dependency>
```

### 2. Configuration Changes

**Before (AWS):**
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**After (Azure):**
```properties
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

### 3. Service Implementation Changes

- `AwsS3Service` → `AzureStorageService`
- `S3FileProcessingService` → `AzureFileProcessingService`
- `AwsS3Config` → `AzureStorageConfig`

### 4. API Changes

- URLs changed from `/s3/*` to `/storage/*` for better generic naming
- Storage type changed from "s3" to "azure" in processing messages

## Azure Storage Setup

### Prerequisites

1. **Azure Storage Account**: Create an Azure Storage Account in your Azure subscription
2. **Container**: Create a blob container within the storage account
3. **Authentication**: Set up managed identity or service principal authentication

### Configuration Steps

1. **Create Azure Storage Account**:
   ```bash
   az storage account create \
     --name yourstorageaccount \
     --resource-group your-resource-group \
     --location eastus \
     --sku Standard_LRS
   ```

2. **Create Container**:
   ```bash
   az storage container create \
     --name your-container-name \
     --account-name yourstorageaccount
   ```

3. **Set up Authentication**:
   
   **Option A: Managed Identity (Recommended for Azure deployments)**
   - Enable managed identity for your Azure App Service or Virtual Machine
   - Grant "Storage Blob Data Contributor" role to the managed identity
   
   **Option B: Service Principal**
   - Create a service principal and note the client ID, tenant ID, and client secret
   - Set environment variables:
     ```bash
     export AZURE_CLIENT_ID=your-client-id
     export AZURE_TENANT_ID=your-tenant-id
     export AZURE_CLIENT_SECRET=your-client-secret
     ```

4. **Update Application Configuration**:
   ```properties
   azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
   azure.storage.container=your-container-name
   ```

## Authentication Methods

The application uses Azure's `DefaultAzureCredential` which automatically detects and uses the best available authentication method in the following order:

1. **Environment Variables** (AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET)
2. **Managed Identity** (when deployed to Azure)
3. **Azure CLI** (for local development)
4. **Visual Studio Code** (for local development)
5. **IntelliJ IDEA** (for local development)

## Local Development

For local development, you can use Azure CLI authentication:

1. Install Azure CLI
2. Login: `az login`
3. The application will automatically use your Azure CLI credentials

## Deployment

### Environment Variables

Set the following environment variables for your deployed application:

```bash
# Required
AZURE_STORAGE_ENDPOINT=https://yourstorageaccount.blob.core.windows.net
AZURE_STORAGE_CONTAINER=your-container-name

# Optional (if not using managed identity)
AZURE_CLIENT_ID=your-client-id
AZURE_TENANT_ID=your-tenant-id
AZURE_CLIENT_SECRET=your-client-secret
```

### Azure App Service Deployment

1. Deploy the application to Azure App Service
2. Enable managed identity for the App Service
3. Grant appropriate permissions to the storage account
4. Set application settings in the Azure portal

## Testing the Migration

1. **Compile the application**:
   ```bash
   ./mvnw compile
   ```

2. **Run tests** (requires PostgreSQL and RabbitMQ):
   ```bash
   ./mvnw test
   ```

3. **Start the application**:
   ```bash
   ./mvnw spring-boot:run -pl web
   ./mvnw spring-boot:run -pl worker
   ```

4. **Access the application**:
   - Navigate to `http://localhost:8080/storage`
   - Upload test images
   - Verify thumbnail generation works

## Backward Compatibility

- Database field names (`s3Key`, `s3Url`) are maintained for backward compatibility
- No database migration is required
- Existing data in the database will continue to work

## Troubleshooting

### Common Issues

1. **Authentication Errors**:
   - Ensure proper Azure credentials are configured
   - Check Azure RBAC permissions on the storage account

2. **Container Not Found**:
   - Verify the container exists in the storage account
   - Check the container name in configuration

3. **Permission Denied**:
   - Ensure the identity has "Storage Blob Data Contributor" role
   - Check network access rules on the storage account

### Useful Azure CLI Commands

```bash
# List storage accounts
az storage account list

# Show storage account details
az storage account show --name yourstorageaccount

# List containers
az storage container list --account-name yourstorageaccount

# Test blob operations
az storage blob upload \
  --account-name yourstorageaccount \
  --container-name your-container-name \
  --name test.txt \
  --file local-file.txt
```

## Performance Considerations

- Azure Storage Blob provides similar performance characteristics to AWS S3
- Consider using Azure CDN for improved global performance
- Use appropriate access tiers (Hot, Cool, Archive) based on usage patterns
- Monitor costs using Azure Cost Management

## Security Best Practices

1. **Use Managed Identity** when deploying to Azure
2. **Limit network access** using firewalls and private endpoints
3. **Enable audit logging** for compliance requirements
4. **Use SAS tokens** for temporary access when needed
5. **Regularly rotate** service principal secrets if used

## Monitoring and Logging

- Enable Azure Storage Analytics for detailed logging
- Use Azure Monitor for performance metrics
- Set up alerts for storage account health
- Monitor application logs for Azure Storage operations