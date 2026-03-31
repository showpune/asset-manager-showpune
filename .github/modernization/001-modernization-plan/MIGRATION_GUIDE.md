# Migration Guide: AWS S3 to Azure Blob Storage

## Overview

This document provides guidance for migrating from AWS S3 to Azure Blob Storage for the Asset Manager application.

## Changes Made

### 1. Dependencies

Added Azure Blob Storage SDK dependencies to both `web` and `worker` modules:
- `azure-storage-blob` (version 12.29.0)
- `azure-identity` (version 1.14.2) for managed identity authentication

### 2. Configuration Classes

#### Web Module
- **Created**: `AzureBlobConfig.java` - Configures Azure Blob Storage client with managed identity authentication
- **Profile**: `azure` - Activated when running with Azure profile

#### Worker Module
- **Created**: `AzureBlobConfig.java` - Configures Azure Blob Storage client with managed identity authentication
- **Profile**: `azure` - Activated when running with Azure profile

### 3. Service Implementations

#### Web Module
- **Created**: `AzureBlobService.java` - Implements `StorageService` interface using Azure Blob Storage SDK
- **Replaces**: `AwsS3Service.java` (when azure profile is active)
- **Features**:
  - Upload objects to Azure Blob Storage
  - List objects from Azure Blob Storage
  - Download objects from Azure Blob Storage
  - Delete objects from Azure Blob Storage
  - Generate blob URLs

#### Worker Module
- **Created**: `AzureBlobProcessingService.java` - Extends `AbstractFileProcessingService` using Azure Blob Storage SDK
- **Replaces**: `S3FileProcessingService.java` (when azure profile is active)
- **Features**:
  - Download original images
  - Upload thumbnails
  - Generate blob URLs

### 4. Application Properties

Added Azure configuration properties:
```properties
azure.storage.account-name=your-storage-account-name
azure.storage.container-name=your-container-name
```

## Authentication

The application uses **Managed Identity** authentication via `DefaultAzureCredential`. This provides:
- Password-less authentication
- Automatic credential discovery
- Support for multiple Azure authentication methods

### Required Azure RBAC Roles

The application's managed identity must be assigned the following roles:
- **Storage Blob Data Contributor** - For full access to blob storage operations (read, write, delete)

## Deployment Configuration

### Environment Variables

Set the following environment variables in your Azure deployment:

```bash
# Azure Blob Storage Configuration
AZURE_STORAGE_ACCOUNT_NAME=<your-storage-account-name>
AZURE_STORAGE_CONTAINER_NAME=<your-container-name>

# Spring Profile
SPRING_PROFILES_ACTIVE=azure
```

### Azure Resources Required

1. **Azure Storage Account**
   - Standard or Premium tier
   - Hot or Cool access tier (depending on workload)
   - Create a blob container with the name specified in `AZURE_STORAGE_CONTAINER_NAME`

2. **Managed Identity**
   - System-assigned or user-assigned managed identity
   - Assigned to the App Service, Container App, or AKS pod
   - Granted **Storage Blob Data Contributor** role on the storage account

## Migration Steps

1. **Create Azure Storage Account**
   ```bash
   az storage account create \
     --name <storage-account-name> \
     --resource-group <resource-group> \
     --location <location> \
     --sku Standard_LRS
   ```

2. **Create Blob Container**
   ```bash
   az storage container create \
     --name <container-name> \
     --account-name <storage-account-name>
   ```

3. **Migrate Existing Data** (if needed)
   - Use AzCopy to copy data from S3 to Azure Blob Storage
   - Or use Azure Data Factory for large-scale migrations

4. **Configure Managed Identity**
   - Enable managed identity on your Azure service
   - Assign the Storage Blob Data Contributor role

5. **Deploy Application**
   - Set environment variables
   - Activate `azure` Spring profile
   - Deploy the application

## Testing

### Local Testing

For local development without Azure resources:
- Keep the `dev` profile active (uses local file storage)
- Or use Azurite (Azure Storage Emulator)

### Azure Testing

To test with actual Azure resources:
1. Authenticate using Azure CLI: `az login`
2. Set environment variables
3. Run with `azure` profile: `./mvnw spring-boot:run -Dspring-boot.run.profiles=azure`

## API Compatibility

The StorageService interface remains unchanged, so the REST API endpoints continue to work without modifications:
- `GET /` - View uploaded images
- `POST /upload` - Upload new images
- `GET /download/{key}` - Download images
- `DELETE /delete/{key}` - Delete images

## Performance Considerations

- Azure Blob Storage provides similar performance to AWS S3
- Consider using Cool tier for infrequently accessed data
- Use Premium tier for low-latency requirements
- Consider enabling CDN for frequently accessed content

## Cost Optimization

- Use lifecycle management policies to automatically tier or delete old blobs
- Enable soft delete for data protection
- Monitor storage metrics to optimize costs

## Monitoring

Monitor the following metrics in Azure:
- **Blob Storage Metrics**: Transactions, Latency, Availability
- **Application Insights**: Track blob operations performance
- **Azure Monitor**: Set up alerts for failures

## Rollback Plan

If issues occur after migration:
1. Switch back to AWS S3 by removing `azure` from active profiles
2. Ensure AWS credentials are still configured
3. Redeploy with default profile

## Support

For issues or questions:
- Review Azure Blob Storage documentation
- Check application logs
- Contact the development team
