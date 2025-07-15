# Azure Storage Account Migration Guide

This guide explains how to migrate from AWS S3 to Azure Storage Account for the Asset Manager application.

## Overview

The Asset Manager has been migrated to use Azure Storage Account instead of AWS S3. The migration includes:

- **Azure Storage Blob**: Replaces AWS S3 for image storage
- **Managed Identity Authentication**: Uses Azure DefaultAzureCredential for secure access
- **Backward Compatibility**: AWS S3 implementation still available via profiles
- **Generic Data Models**: Updated to support multiple storage providers

## Configuration

### Azure Storage Configuration

Add the following properties to your `application.properties`:

```properties
# Azure Storage Account Configuration  
azure.storage.account.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container.name=images
```

### Environment Variables

For production deployments with Managed Identity:

```bash
# Optional: Specify client ID if using user-assigned managed identity
export AZURE_CLIENT_ID=your-managed-identity-client-id
```

### Spring Profiles

The application supports multiple profiles:

- **Default Profile**: Uses Azure Storage Account + RabbitMQ
- **AWS Profile**: Uses AWS S3 + RabbitMQ (legacy mode)
- **Dev Profile**: Uses local file storage + RabbitMQ

To use AWS S3 (legacy mode):
```bash
java -jar app.jar --spring.profiles.active=aws
```

To use local development mode:
```bash
java -jar app.jar --spring.profiles.active=dev
```

## Authentication

### Azure Managed Identity (Recommended)

The application uses Azure DefaultAzureCredential which supports:

1. **Environment Variables**: `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`
2. **Managed Identity**: System-assigned or user-assigned managed identity
3. **Azure CLI**: For development environments
4. **Visual Studio Code**: For development environments
5. **IntelliJ IDEA**: For development environments

### Development Setup

For local development, use Azure CLI:

```bash
# Login to Azure
az login

# Set subscription (if needed)
az account set --subscription "your-subscription-id"
```

## Dependencies

The following Azure dependencies have been added:

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

## Service Implementation

### AzureStorageService

Implements the same `StorageService` interface as the AWS S3 service:

- `listObjects()`: Lists all blobs in the container
- `uploadObject(MultipartFile)`: Uploads files to Azure Storage
- `getObject(String)`: Downloads files from Azure Storage  
- `deleteObject(String)`: Deletes files from Azure Storage
- `getStorageType()`: Returns "azure"

### AzureFileProcessingService

Extends `AbstractFileProcessingService` for thumbnail generation:

- `downloadOriginal()`: Downloads original image from Azure Storage
- `uploadThumbnail()`: Uploads thumbnail to Azure Storage
- `generateUrl()`: Generates blob URL

## Migration Steps

1. **Create Azure Storage Account**
   ```bash
   az storage account create \
     --name yourstorageaccount \
     --resource-group your-resource-group \
     --location eastus \
     --sku Standard_LRS
   ```

2. **Create Container**
   ```bash
   az storage container create \
     --name images \
     --account-name yourstorageaccount
   ```

3. **Set up Managed Identity** (for Azure App Service/Container Apps)
   ```bash
   az webapp identity assign \
     --name your-app-name \
     --resource-group your-resource-group
   ```

4. **Grant Storage Permissions**
   ```bash
   az role assignment create \
     --assignee <managed-identity-object-id> \
     --role "Storage Blob Data Contributor" \
     --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account>
   ```

5. **Update Application Configuration**
   - Update `azure.storage.account.endpoint` 
   - Update `azure.storage.container.name`
   - Remove AWS configuration (optional)

6. **Deploy Application**
   - Deploy with default profile (Azure Storage is now default)
   - Test image upload/download functionality

## Verification

Test the migration by:

1. **Upload Test**: Upload an image through the web interface
2. **List Test**: Verify images appear in the gallery
3. **Download Test**: Click on an image to view it
4. **Thumbnail Test**: Verify thumbnails are generated
5. **Delete Test**: Delete an image and verify it's removed

## Troubleshooting

### Authentication Issues

```
DefaultAzureCredentialBuilder failed to retrieve credentials
```

**Solution**: Ensure managed identity is properly configured or Azure CLI is logged in for development.

### Container Not Found

```
BlobContainerClient: Container 'images' not found
```

**Solution**: The application will create the container automatically if it doesn't exist. Ensure the managed identity has sufficient permissions.

### Permission Issues

```
Access denied when accessing blob storage
```

**Solution**: Verify the managed identity has "Storage Blob Data Contributor" role on the storage account.

## Performance Considerations

- **Connection Pooling**: Azure Storage Blob SDK uses connection pooling by default
- **Retry Policies**: Built-in exponential backoff for transient failures
- **Parallel Uploads**: Large files are automatically uploaded in parallel chunks
- **Caching**: Consider implementing CDN for frequently accessed images

## Rollback Plan

To rollback to AWS S3:

1. Set Spring profile to `aws`:
   ```bash
   java -jar app.jar --spring.profiles.active=aws
   ```

2. Ensure AWS configuration is still present in `application.properties`

3. Verify AWS IAM permissions are still valid

4. Test functionality with AWS S3

Both implementations can run simultaneously during migration period.