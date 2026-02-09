# AWS S3 to Azure Blob Storage Migration Guide

## Overview

This document provides instructions for migrating from AWS S3 to Azure Blob Storage in the asset-manager application. The migration has been implemented using a profile-based approach, allowing you to choose between AWS S3, Azure Blob Storage, or local file storage.

## Changes Made

### 1. Dependencies Added

**Web Module (`web/pom.xml`):**
- `com.azure:azure-storage-blob:12.29.0` - Azure Blob Storage SDK
- `com.azure:azure-identity:1.15.1` - Azure Identity SDK for managed identity authentication

**Worker Module (`worker/pom.xml`):**
- Same Azure dependencies as web module

### 2. New Azure Blob Storage Classes

**Web Module:**
- `AzureBlobConfig.java` - Configuration for Azure Blob Storage using managed identity
- `AzureBlobService.java` - Implementation of StorageService interface for Azure Blob Storage

**Worker Module:**
- `AzureBlobConfig.java` - Configuration for Azure Blob Storage using managed identity
- `AzureBlobFileProcessingService.java` - Implementation for thumbnail processing with Azure Blob Storage

### 3. Profile-Based Configuration

The application now supports three storage profiles:

- **`dev`** - Local file storage (for development)
- **`aws`** - AWS S3 storage (existing implementation)
- **`azure`** - Azure Blob Storage (new implementation)

## Deployment Configuration

### Azure Blob Storage (Recommended)

To use Azure Blob Storage, configure the following environment variables:

```bash
# Azure Storage Configuration
AZURE_STORAGE_ACCOUNT_NAME=<your-storage-account-name>
AZURE_STORAGE_CONTAINER_NAME=<your-container-name>

# Spring Profile
SPRING_PROFILES_ACTIVE=azure
```

**Authentication:**
The application uses Azure Managed Identity (`DefaultAzureCredential`) for passwordless authentication. Ensure your Azure resource (App Service, Container Apps, VM, etc.) has a managed identity with the following permissions:
- Storage Blob Data Contributor (or Storage Blob Data Reader/Writer)

### AWS S3 (For Backward Compatibility)

To continue using AWS S3, configure the following environment variables:

```bash
# AWS Configuration
AWS_ACCESS_KEY=<your-access-key>
AWS_SECRET_KEY=<your-secret-key>
AWS_REGION=<your-region>
AWS_S3_BUCKET=<your-bucket-name>

# Spring Profile
SPRING_PROFILES_ACTIVE=aws
```

### Local File Storage (Development)

For local development without cloud storage:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

## Migration Steps

### Step 1: Create Azure Storage Account

1. Create an Azure Storage Account:
   ```bash
   az storage account create \
     --name <storage-account-name> \
     --resource-group <resource-group> \
     --location <location> \
     --sku Standard_LRS \
     --kind StorageV2
   ```

2. Create a blob container:
   ```bash
   az storage container create \
     --name <container-name> \
     --account-name <storage-account-name> \
     --auth-mode login
   ```

### Step 2: Configure Managed Identity

1. Enable system-assigned managed identity on your Azure resource (e.g., App Service):
   ```bash
   az webapp identity assign \
     --name <app-name> \
     --resource-group <resource-group>
   ```

2. Grant the managed identity access to the storage account:
   ```bash
   # Get the principal ID
   PRINCIPAL_ID=$(az webapp identity show \
     --name <app-name> \
     --resource-group <resource-group> \
     --query principalId -o tsv)
   
   # Assign Storage Blob Data Contributor role
   az role assignment create \
     --assignee $PRINCIPAL_ID \
     --role "Storage Blob Data Contributor" \
     --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account-name>
   ```

### Step 3: Migrate Existing Data (Optional)

If you have existing data in S3, use AzCopy to migrate it:

```bash
# Install AzCopy
# Download from https://aka.ms/downloadazcopy

# Login to Azure
azcopy login

# Copy from S3 to Azure Blob Storage
azcopy copy \
  "https://s3.amazonaws.com/<bucket-name>/*" \
  "https://<storage-account-name>.blob.core.windows.net/<container-name>/" \
  --recursive
```

### Step 4: Update Application Configuration

Update your application's environment variables to use Azure:

```bash
AZURE_STORAGE_ACCOUNT_NAME=<your-storage-account-name>
AZURE_STORAGE_CONTAINER_NAME=<your-container-name>
SPRING_PROFILES_ACTIVE=azure
```

### Step 5: Restart Application

Restart your application to apply the new configuration.

## Testing

### Local Testing with Azurite

For local development and testing, you can use Azurite (Azure Storage Emulator):

1. Install Azurite:
   ```bash
   npm install -g azurite
   ```

2. Start Azurite:
   ```bash
   azurite --silent --location /tmp/azurite --debug /tmp/azurite/debug.log
   ```

3. Configure application to use Azurite:
   ```bash
   AZURE_STORAGE_ACCOUNT_NAME=devstoreaccount1
   AZURE_STORAGE_CONTAINER_NAME=test-container
   SPRING_PROFILES_ACTIVE=azure
   ```

## Security Considerations

1. **Managed Identity**: The application uses Azure Managed Identity for passwordless authentication, eliminating the need to store credentials in code or configuration.

2. **Filename Sanitization**: The Azure implementation includes filename sanitization to prevent path traversal attacks:
   ```java
   String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
   ```

3. **RBAC**: Use Azure RBAC to grant least-privilege access to storage resources.

## API Compatibility

The migration maintains API compatibility with the existing S3 implementation:
- `listObjects()` - Lists all blobs in the container
- `uploadObject(MultipartFile)` - Uploads a file to blob storage
- `getObject(String key)` - Downloads a blob by key
- `deleteObject(String key)` - Deletes a blob and its thumbnail

## Troubleshooting

### Issue: "No qualifying bean of type StorageService"

**Cause**: No storage profile is active.

**Solution**: Set `SPRING_PROFILES_ACTIVE` to one of: `dev`, `aws`, or `azure`.

### Issue: Authentication failures with Azure

**Cause**: Managed identity not configured or insufficient permissions.

**Solution**:
1. Verify managed identity is enabled on your Azure resource
2. Verify the identity has "Storage Blob Data Contributor" role
3. Check Azure AD role assignments:
   ```bash
   az role assignment list --assignee <principal-id>
   ```

### Issue: Container not found

**Cause**: Container doesn't exist in the storage account.

**Solution**: Create the container:
```bash
az storage container create \
  --name <container-name> \
  --account-name <storage-account-name> \
  --auth-mode login
```

## Performance Considerations

- Azure Blob Storage provides similar performance to S3 for most workloads
- Consider using Azure CDN for frequently accessed content
- Use hot, cool, or archive tiers based on access patterns

## Cost Optimization

- Use lifecycle management policies to automatically tier or delete old blobs
- Enable soft delete for data protection
- Monitor storage metrics to optimize capacity

## References

- [Azure Blob Storage Documentation](https://docs.microsoft.com/en-us/azure/storage/blobs/)
- [Azure Identity SDK](https://docs.microsoft.com/en-us/java/api/overview/azure/identity-readme)
- [Migrate from AWS S3 to Azure Blob Storage](https://docs.microsoft.com/en-us/azure/storage/common/storage-migration-to-azure-from-aws)
