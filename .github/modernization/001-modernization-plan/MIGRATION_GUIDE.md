# Azure Blob Storage Migration Guide

## Overview

This guide documents the migration from AWS S3 to Azure Blob Storage for the assets-manager application.

## Changes Made

### 1. Dependencies

Added Azure Blob Storage SDK dependencies to both web and worker modules:

```xml
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-storage-blob</artifactId>
    <version>12.29.0</version>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.14.2</version>
</dependency>
```

### 2. Configuration

#### Web Module
- Created `AzureBlobConfig.java` - Configures BlobServiceClient with managed identity authentication
- Created `application-azure.properties` - Azure-specific configuration

#### Worker Module
- Created `AzureBlobConfig.java` - Configures BlobServiceClient with managed identity authentication
- Created `application-azure.properties` - Azure-specific configuration

### 3. Service Implementation

#### Web Module
- Created `AzureBlobService.java` - Implements StorageService interface using Azure Blob Storage
  - `listObjects()` - Lists blobs in container
  - `uploadObject()` - Uploads files to Azure Blob Storage
  - `getObject()` - Downloads blobs
  - `deleteObject()` - Deletes blobs

#### Worker Module
- Created `AzureBlobFileProcessingService.java` - Implements AbstractFileProcessingService for Azure
  - `downloadOriginal()` - Downloads original images
  - `uploadThumbnail()` - Uploads generated thumbnails
  - Uses Azure Blob Storage SDK

## Authentication

The application uses **Azure Managed Identity** for authentication via `DefaultAzureCredential`:
- No credentials stored in code or configuration
- Automatically uses the managed identity assigned to the Azure resource
- Falls back to other authentication methods in development (Azure CLI, Visual Studio, etc.)

## Required Azure Resources

### Storage Account
- Create an Azure Storage Account
- Create a container (default name: "assets")

### Required RBAC Roles
Assign these roles to the application's managed identity:
- **Storage Blob Data Contributor** - For read, write, and delete operations on blobs

## Environment Variables

Set the following environment variables:

```bash
# Azure Blob Storage
AZURE_STORAGE_ACCOUNT_NAME=<your-storage-account-name>
AZURE_STORAGE_CONTAINER_NAME=assets  # Optional, defaults to "assets"
```

## Running the Application

### With Azure Profile

To use Azure Blob Storage instead of AWS S3:

```bash
java -jar app.jar --spring.profiles.active=azure
```

Or set environment variable:
```bash
export SPRING_PROFILES_ACTIVE=azure
java -jar app.jar
```

### Profile-based Service Selection

The application automatically selects the correct storage implementation based on the active profile:
- **No profile or `!azure`**: Uses AWS S3 (AwsS3Service, S3FileProcessingService)
- **Profile `azure`**: Uses Azure Blob Storage (AzureBlobService, AzureBlobFileProcessingService)
- **Profile `dev`**: Uses local file storage (LocalFileStorageService, LocalFileProcessingService)

## Migration from S3 to Azure Blob Storage

### Data Migration

To migrate existing data from S3 to Azure Blob Storage, use **AzCopy**:

```bash
# Install AzCopy
# https://docs.microsoft.com/en-us/azure/storage/common/storage-use-azcopy-v10

# Copy from S3 to Azure
azcopy copy \
  "https://<s3-bucket>.s3.amazonaws.com/*" \
  "https://<storage-account>.blob.core.windows.net/<container>?<SAS-token>" \
  --recursive
```

### Database Updates

Update the ImageMetadata records in PostgreSQL:
- The `s3Key`, `s3Url`, `thumbnailKey`, and `thumbnailUrl` columns continue to work with Azure Blob Storage
- URLs will now point to Azure Blob Storage endpoints

## API Compatibility

The StorageService interface remains unchanged, ensuring:
- No changes to controller or business logic
- Existing API endpoints continue to work
- Storage operations are abstracted behind the interface

## Key Differences Between S3 and Azure Blob Storage

| Feature | AWS S3 | Azure Blob Storage |
|---------|--------|-------------------|
| Container | Bucket | Container |
| Object | Object | Blob |
| Authentication | Access Key / IAM | Managed Identity / SAS Token |
| SDK | AWS SDK for Java | Azure SDK for Java |
| URL Format | `s3.amazonaws.com` | `blob.core.windows.net` |

## Testing

Build and test the application:

```bash
# Build
./mvnw clean install

# Test
./mvnw test

# Run with Azure profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=azure
```

## Rollback Plan

If issues occur, rollback is simple:
1. Remove the `azure` profile or switch to default profile
2. Application will automatically use AWS S3
3. No code changes required

## Additional Notes

- Both S3 and Azure implementations can coexist in the same application
- Switch between them using Spring profiles
- Managed identity provides better security than access keys
- No credentials stored in configuration files
