# S3 to Azure Blob Storage Migration Guide

This document describes the migration from AWS S3 to Azure Blob Storage that has been completed in this project.

## Overview

The asset management system has been successfully migrated from AWS S3 to Azure Blob Storage while maintaining all existing functionality. The migration includes:

- **Storage Layer**: AWS S3 → Azure Blob Storage
- **Authentication**: AWS Access Key/Secret → Azure Managed Identity
- **Dependencies**: AWS SDK → Azure SDK for Java
- **Configuration**: S3 bucket settings → Azure storage container settings

## Key Changes

### 1. Dependencies

**Removed AWS Dependencies:**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

**Added Azure Dependencies:**
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

### 2. Configuration Classes

**Replaced:**
- `AwsS3Config` → `AzureBlobStorageConfig`

**New Configuration:**
```java
@Configuration
public class AzureBlobStorageConfig {
    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
}
```

### 3. Service Implementations

**Replaced:**
- `AwsS3Service` → `AzureBlobStorageService`
- `S3FileProcessingService` → `AzureBlobFileProcessingService`

### 4. Model Changes

**Renamed for Storage Agnostic Approach:**
- `S3StorageItem` → `StorageItem`
- `ImageMetadata.s3Key` → `ImageMetadata.blobKey`
- `ImageMetadata.s3Url` → `ImageMetadata.blobUrl`

### 5. Configuration Properties

**Before (AWS S3):**
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**After (Azure Blob Storage):**
```properties
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

## Authentication

The migration uses **Azure Managed Identity** for authentication, which is more secure than access keys:

- **No credentials** need to be stored in configuration files
- **Automatic token management** by Azure
- **Role-based access control** through Azure RBAC

## Functionality Preserved

All existing functionality has been preserved:

✅ **File Upload**: Upload images to Azure Blob Storage  
✅ **File Download**: Download images from Azure Blob Storage  
✅ **File Listing**: List all stored images  
✅ **File Deletion**: Delete images and their thumbnails  
✅ **Thumbnail Generation**: Worker service processes images  
✅ **Metadata Storage**: PostgreSQL metadata storage  
✅ **Message Queuing**: RabbitMQ for async processing  
✅ **URL Generation**: Generate public URLs for blob access  

## Storage Interface

The `StorageService` interface remains unchanged, ensuring:
- **Zero impact** on controllers and business logic
- **Clean separation** between storage implementation and application logic
- **Easy switching** between storage providers in different environments

## Deployment Requirements

To deploy this migrated application:

1. **Azure Storage Account**: Create an Azure Storage Account
2. **Blob Container**: Create a container within the storage account
3. **Managed Identity**: Configure managed identity for the application
4. **RBAC Permissions**: Grant "Storage Blob Data Contributor" role to the managed identity
5. **Configuration**: Update `azure.storage.endpoint` and `azure.storage.container` properties

## Environment Support

The migration supports multiple environments:

- **Development**: Uses `LocalFileStorageService` (file system storage)
- **Production**: Uses `AzureBlobStorageService` (Azure Blob Storage)
- **Testing**: Can use either based on Spring profiles

## Migration Benefits

1. **Security**: Managed Identity eliminates credential management
2. **Scalability**: Azure Blob Storage provides enterprise-scale storage
3. **Cost**: Pay-per-use pricing model
4. **Integration**: Native integration with Azure services
5. **Compliance**: Built-in compliance and security features
6. **Reliability**: High availability and durability guarantees

## Future Considerations

- **Azure Service Bus**: Consider migrating from RabbitMQ to Azure Service Bus for full Azure integration
- **Azure Database**: Consider migrating from PostgreSQL to Azure Database for PostgreSQL
- **Monitoring**: Integrate with Azure Monitor and Application Insights
- **CDN**: Consider Azure CDN for improved image delivery performance

## Troubleshooting

**Common Issues:**

1. **Authentication Errors**: Ensure managed identity is configured with proper RBAC permissions
2. **Container Not Found**: Verify the container name in configuration
3. **Network Issues**: Check NSG rules and firewall settings
4. **Performance**: Consider using Azure CDN for high-traffic scenarios

**Logs to Check:**
- Application logs for Azure SDK errors
- Azure Activity Logs for RBAC issues
- Container logs for deployment problems