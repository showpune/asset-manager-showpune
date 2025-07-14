# Azure Storage Account Migration Guide

This document provides a comprehensive guide for migrating from AWS S3 to Azure Blob Storage.

## Overview

The Asset Manager application has been successfully migrated from AWS S3 to Azure Blob Storage. This migration includes:

- **Dependencies**: Replaced AWS SDK with Azure Storage SDK
- **Configuration**: Updated from AWS credentials to Azure DefaultAzureCredential
- **Services**: Migrated storage services to use Azure Blob Storage APIs
- **Models**: Updated data models to reflect blob storage terminology
- **Controllers**: Updated endpoints from `/s3/*` to `/blob/*`
- **Templates**: Updated UI references and URLs

## Key Changes

### 1. Dependencies Migration

**Before (AWS S3):**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.13</version>
</dependency>
```

**After (Azure Blob Storage):**
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

### 2. Configuration Migration

**Before (AWS S3):**
```properties
# AWS S3 Configuration
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**After (Azure Blob Storage):**
```properties
# Azure Storage Configuration
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

### 3. Client Configuration

**Before (AWS S3):**
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

**After (Azure Blob Storage):**
```java
@Bean
public BlobServiceClient blobServiceClient() {
    return new BlobServiceClientBuilder()
            .endpoint(endpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
}
```

### 4. URL Endpoints Migration

All URL endpoints have been updated:
- `/s3/*` → `/blob/*`
- `/s3/upload` → `/blob/upload`
- `/s3/view/{key}` → `/blob/view/{key}`
- `/s3/delete/{key}` → `/blob/delete/{key}`

## Azure Setup Requirements

### 1. Azure Storage Account

1. Create an Azure Storage Account in your Azure subscription
2. Create a blob container within the storage account
3. Note the storage account endpoint: `https://<storageaccountname>.blob.core.windows.net`

### 2. Authentication Setup

The application uses Azure DefaultAzureCredential, which supports multiple authentication methods:

#### Option 1: Managed Identity (Recommended for Azure deployments)
- Enable system-assigned managed identity on your Azure App Service or VM
- Grant the managed identity "Storage Blob Data Contributor" role on the storage account

#### Option 2: Service Principal
```bash
# Create service principal
az ad sp create-for-rbac --name "asset-manager-sp" --role "Storage Blob Data Contributor" --scopes "/subscriptions/{subscription-id}/resourceGroups/{resource-group}/providers/Microsoft.Storage/storageAccounts/{storage-account}"

# Set environment variables
export AZURE_CLIENT_ID=<client-id>
export AZURE_CLIENT_SECRET=<client-secret>
export AZURE_TENANT_ID=<tenant-id>
```

#### Option 3: Connection String (Development only)
```properties
# For development/testing only - not recommended for production
azure.storage.connectionString=DefaultEndpointsProtocol=https;AccountName=<account>;AccountKey=<key>;EndpointSuffix=core.windows.net
```

### 3. Configuration Update

Update your `application.properties`:

```properties
# Azure Storage Configuration
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

## Migration Steps

### 1. Data Migration (if needed)

If you have existing data in S3, you can migrate it to Azure Blob Storage using:

#### Option 1: Azure Data Factory
- Create a copy pipeline in Azure Data Factory
- Configure S3 as source and Azure Blob Storage as destination

#### Option 2: AzCopy
```bash
# Install AzCopy
# Copy from S3 to Azure Blob Storage
azcopy copy 'https://s3.amazonaws.com/mybucket/*' 'https://mystorageaccount.blob.core.windows.net/mycontainer/' --recursive
```

### 2. Application Deployment

1. Update configuration with Azure Storage details
2. Ensure proper authentication is configured
3. Deploy the updated application
4. Verify functionality by uploading/viewing/deleting images

### 3. Testing Checklist

- [ ] Image upload functionality
- [ ] Image listing/viewing
- [ ] Image deletion
- [ ] Thumbnail generation (worker module)
- [ ] Error handling for missing blobs
- [ ] Authentication with Azure Storage

## Key Differences

| Aspect | AWS S3 | Azure Blob Storage |
|--------|--------|-------------------|
| **Container** | Bucket | Container |
| **Object** | Object/Key | Blob |
| **URL Structure** | `https://bucket.s3.region.amazonaws.com/key` | `https://account.blob.core.windows.net/container/blob` |
| **Authentication** | Access Key + Secret | DefaultAzureCredential |
| **SDK Package** | `software.amazon.awssdk:s3` | `com.azure:azure-storage-blob` |

## Security Considerations

1. **Use Managed Identity** when running on Azure for enhanced security
2. **Avoid connection strings** in production environments
3. **Use SAS tokens** for limited-time access to specific blobs
4. **Enable encryption** at rest and in transit
5. **Configure network access** rules as needed

## Troubleshooting

### Common Issues

1. **Authentication Errors**
   - Verify Azure credentials are properly configured
   - Check that the service principal has correct permissions
   - Ensure the storage account endpoint is correct

2. **Container Not Found**
   - Verify the container name in configuration
   - Ensure the container exists in the storage account

3. **Permission Denied**
   - Check that the identity has "Storage Blob Data Contributor" role
   - Verify network access rules allow your application's IP

### Debug Commands

```bash
# Test Azure CLI authentication
az account show

# List storage accounts
az storage account list

# List containers
az storage container list --account-name <storage-account>

# Test blob upload
az storage blob upload --account-name <storage-account> --container-name <container> --name test.txt --file test.txt
```

## Performance Considerations

1. **Use appropriate access tiers** (Hot, Cool, Cold, Archive) based on access patterns
2. **Configure CDN** for frequently accessed content
3. **Use blob prefixes** to organize content logically
4. **Monitor costs** with Azure Cost Management

## Support and Monitoring

1. **Azure Monitor** for application insights and metrics
2. **Storage Analytics** for detailed storage metrics
3. **Azure Storage Explorer** for GUI-based blob management
4. **Activity logs** for audit and troubleshooting

## Next Steps

1. Set up monitoring and alerts for the Azure Storage Account
2. Configure backup and disaster recovery strategies
3. Implement content delivery network (CDN) if needed
4. Review and optimize storage costs based on access patterns