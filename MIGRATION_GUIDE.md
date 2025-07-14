# Migration Guide: AWS S3 to Azure Storage Account

This document provides a comprehensive guide for migrating from AWS S3 to Azure Storage Account.

## Overview

The asset manager application has been successfully migrated from AWS S3 to Azure Storage Account. This migration includes:

- **Storage Backend**: AWS S3 → Azure Blob Storage  
- **Authentication**: AWS Access Keys → Azure DefaultAzureCredential
- **SDK**: AWS SDK for Java → Azure Storage Blob SDK
- **Configuration**: AWS-specific properties → Azure-specific properties
- **Data Model**: S3-specific field names → Azure Blob-specific field names

## Changes Made

### 1. Dependencies Updated

**Before (AWS S3)**:
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.13</version>
</dependency>
```

**After (Azure Storage)**:
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

**Before (AWS S3)**:
```properties
# AWS S3 Configuration
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**After (Azure Storage)**:
```properties
# Azure Storage Configuration
azure.storage.account-name=your-storage-account-name
azure.storage.endpoint=https://your-storage-account-name.blob.core.windows.net
azure.storage.container-name=your-container-name
```

### 3. Service Implementation Changes

**Before (AWS S3 Client)**:
```java
@Bean
public S3Client s3Client() {
    AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
    return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();
}
```

**After (Azure Blob Client)**:
```java
@Bean
public BlobServiceClient blobServiceClient() {
    return new BlobServiceClientBuilder()
            .endpoint(endpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
}
```

### 4. Data Model Changes

**Database Schema Updates Required**:
```sql
-- Update existing tables to use new field names
ALTER TABLE image_metadata RENAME COLUMN s3_key TO blob_key;
ALTER TABLE image_metadata RENAME COLUMN s3_url TO blob_url;
```

### 5. API Changes

**Storage Operations**:

| Operation | AWS S3 | Azure Storage |
|-----------|---------|---------------|
| Upload | `s3Client.putObject()` | `blobClient.upload()` |
| Download | `s3Client.getObject()` | `blobClient.openInputStream()` |
| Delete | `s3Client.deleteObject()` | `blobClient.deleteIfExists()` |
| List | `s3Client.listObjectsV2()` | `containerClient.listBlobs()` |
| Get URL | `s3Client.utilities().getUrl()` | `blobClient.getBlobUrl()` |

### 6. URL Endpoints Updated

All web endpoints changed from `/s3/*` to `/storage/*`:

- `/s3` → `/storage`
- `/s3/upload` → `/storage/upload`
- `/s3/view/{key}` → `/storage/view/{key}`
- `/s3/delete/{key}` → `/storage/delete/{key}`

## Azure Setup Steps

### 1. Create Azure Storage Account

```bash
# Using Azure CLI
az storage account create \
  --name yourstorageaccount \
  --resource-group your-resource-group \
  --location eastus \
  --sku Standard_LRS
```

### 2. Create Blob Container

```bash
az storage container create \
  --name your-container-name \
  --account-name yourstorageaccount
```

### 3. Set Up Authentication

#### Option A: Local Development (Azure CLI)
```bash
az login
```

#### Option B: Azure Hosting (Managed Identity)
1. Enable System Assigned Managed Identity on your Azure App Service
2. Grant Storage Blob Data Contributor role to the Managed Identity:

```bash
az role assignment create \
  --assignee <managed-identity-principal-id> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account>
```

#### Option C: Service Principal
```bash
# Create service principal
az ad sp create-for-rbac --name "asset-manager-sp"

# Set environment variables
export AZURE_CLIENT_ID=<service-principal-id>
export AZURE_CLIENT_SECRET=<service-principal-secret>
export AZURE_TENANT_ID=<tenant-id>

# Grant role
az role assignment create \
  --assignee <service-principal-id> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account>
```

## Migration Checklist

- [x] Update Maven dependencies in both web and worker modules
- [x] Replace AWS configuration classes with Azure configuration classes
- [x] Update application.properties files
- [x] Migrate service implementations from S3 to Azure Blob Storage
- [x] Update data models and field names
- [x] Update controller endpoints from `/s3` to `/storage`
- [x] Update HTML templates with new endpoint URLs
- [x] Update storage type from "s3" to "azure" in message processing
- [x] Test compilation and packaging
- [ ] Update database schema (manual step)
- [ ] Deploy to Azure with proper authentication setup
- [ ] Test end-to-end functionality

## Testing the Migration

### 1. Local Development Testing

1. Set up Azure Storage Account as described above
2. Update configuration files with your Azure Storage details
3. Run the application locally:
   ```bash
   cd web
   ../mvnw spring-boot:run
   ```
4. Test upload, view, and delete operations through the web interface

### 2. Azure Deployment Testing

1. Deploy both web and worker modules to Azure App Service
2. Enable Managed Identity on both App Services
3. Grant Storage Blob Data Contributor role to both Managed Identities
4. Test all functionality in the cloud environment

## Rollback Plan

If you need to rollback to AWS S3:

1. Revert the code changes by checking out the previous S3-based commit
2. Update configuration files with AWS credentials
3. Migrate data from Azure Storage back to S3 if necessary
4. Update database schema to revert field name changes:
   ```sql
   ALTER TABLE image_metadata RENAME COLUMN blob_key TO s3_key;
   ALTER TABLE image_metadata RENAME COLUMN blob_url TO s3_url;
   ```

## Security Considerations

### Azure Storage Security Features

1. **Azure AD Integration**: Uses DefaultAzureCredential for secure authentication
2. **Managed Identity**: No secrets stored in application code when using Managed Identity
3. **RBAC**: Fine-grained role-based access control
4. **Private Endpoints**: Can restrict access to specific virtual networks
5. **Encryption**: Data encrypted at rest and in transit by default

### Best Practices

1. Use Managed Identity in production environments
2. Grant minimal required permissions (Storage Blob Data Contributor)
3. Enable Azure Storage logging and monitoring
4. Consider using Azure Private Endpoints for additional security
5. Regularly rotate Service Principal secrets if used

## Performance Considerations

1. **Azure Storage Tiers**: Consider using appropriate storage tiers (Hot/Cool/Archive) based on access patterns
2. **CDN Integration**: Use Azure CDN for better global performance
3. **Connection Pooling**: Azure SDK handles connection pooling automatically
4. **Batch Operations**: Consider using batch operations for bulk uploads/deletes

## Monitoring and Troubleshooting

1. **Azure Monitor**: Use Azure Monitor to track storage metrics
2. **Application Insights**: Monitor application performance and errors
3. **Storage Analytics**: Enable storage analytics for detailed logs
4. **Health Checks**: Implement health checks for storage connectivity

```java
// Example health check
@Component
public class AzureStorageHealthIndicator implements HealthIndicator {
    
    @Autowired
    private BlobServiceClient blobServiceClient;
    
    @Override
    public Health health() {
        try {
            blobServiceClient.getProperties();
            return Health.up()
                .withDetail("storage", "Azure Storage is accessible")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("storage", "Azure Storage is not accessible")
                .withException(e)
                .build();
        }
    }
}
```