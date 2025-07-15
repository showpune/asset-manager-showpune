# Azure Storage Migration Guide

This document outlines the migration from AWS S3 to Azure Storage for the Asset Manager application.

## Changes Made

### 1. Dependencies Updated
- **Removed**: `software.amazon.awssdk:s3`
- **Added**: 
  - `com.azure:azure-storage-blob:12.29.0`
  - `com.azure:azure-storage-blob-batch:12.25.0`
  - `com.azure:azure-identity:1.15.4`

### 2. Configuration
- **Created**: `AzureStorageConfig.java` (replaces `AwsS3Config.java`)
- **Uses**: `DefaultAzureCredential` for authentication
- **Properties**:
  ```properties
  azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
  azure.storage.container=your-container-name
  ```

### 3. Services
- **Created**: `AzureBlobService.java` (replaces `AwsS3Service.java`)
- **Created**: `AzureBlobFileProcessingService.java` (replaces `S3FileProcessingService.java`)
- **API Mapping**:
  - `S3Client.listObjectsV2()` → `BlobContainerClient.listBlobs()`
  - `S3Client.putObject()` → `BlobClient.upload()`
  - `S3Client.getObject()` → `BlobClient.openInputStream()`
  - `S3Client.deleteObject()` → `BlobClient.deleteIfExists()`

### 4. Models
- **Renamed**: `S3StorageItem` → `StorageItem`
- **Updated**: `ImageMetadata` fields:
  - `s3Key` → `storageKey`
  - `s3Url` → `storageUrl`

### 5. Controller & Routes
- **Renamed**: `S3Controller` → `StorageController`
- **New Routes**: `/storage/*` (replaces `/s3/*`)
- **Backward Compatibility**: Added redirects from `/s3/*` to `/storage/*`

### 6. Templates
- **Updated**: All HTML templates to use `/storage` routes
- **Rebranded**: "AWS S3 Asset Manager" → "Azure Storage Asset Manager"

## Azure Authentication

The application uses `DefaultAzureCredential` which supports multiple authentication methods:

1. **Environment Variables**: `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`
2. **Managed Identity**: For Azure-hosted services
3. **Azure CLI**: For local development (`az login`)
4. **Visual Studio Code**: For development
5. **IntelliJ**: For development

## Deployment Configuration

### Environment Variables
```bash
# Azure Storage
AZURE_STORAGE_ENDPOINT=https://yourstorageaccount.blob.core.windows.net
AZURE_STORAGE_CONTAINER=your-container-name

# For Service Principal Authentication (if not using Managed Identity)
AZURE_CLIENT_ID=your-client-id
AZURE_CLIENT_SECRET=your-client-secret
AZURE_TENANT_ID=your-tenant-id
```

### Application Properties
```properties
# Azure Storage Configuration
azure.storage.endpoint=${AZURE_STORAGE_ENDPOINT:https://yourstorageaccount.blob.core.windows.net}
azure.storage.container=${AZURE_STORAGE_CONTAINER:your-container-name}
```

## Testing

1. **Local Development**: Use `az login` for authentication
2. **Build**: `mvn clean compile` (should succeed without Azure credentials)
3. **Run**: Requires valid Azure Storage account and credentials

## Backward Compatibility

- All original `/s3/*` routes redirect to `/storage/*`
- Original S3 code preserved as `.bak` files
- Database schema remains compatible (uses generic `storageKey`/`storageUrl` fields)

## Rollback Plan

If needed, the rollback process involves:
1. Restore `.bak` files to original names
2. Update dependencies back to AWS S3 SDK
3. Revert configuration and templates
4. Update database field names back to S3-specific names

## Next Steps

1. Configure Azure Storage account and container
2. Set up authentication (Managed Identity recommended for production)
3. Test file upload, download, and deletion operations
4. Verify thumbnail generation in worker module
5. Remove `.bak` files once confident in migration