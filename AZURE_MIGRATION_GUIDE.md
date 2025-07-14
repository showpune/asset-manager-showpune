# Azure Storage Account Migration Guide

This document provides instructions for migrating from AWS S3 to Azure Storage Account.

## Overview

The application has been updated to support both AWS S3 (legacy) and Azure Blob Storage through Spring profiles. This allows for a smooth migration path and testing.

## Configuration

### Azure Storage Account Setup

1. **Create an Azure Storage Account**
   - Create a new Azure Storage Account in your Azure subscription
   - Note the storage account name (e.g., `mystorageaccount`)
   - The endpoint will be: `https://mystorageaccount.blob.core.windows.net`

2. **Create a Blob Container**
   - Create a container named `assets` (or configure a different name)
   - Set the access level as appropriate for your use case

3. **Authentication Setup**
   - The application uses Azure Managed Identity authentication
   - Ensure your application service has appropriate permissions to the storage account
   - For local development, ensure you're logged in with Azure CLI or have appropriate credentials

### Application Configuration

#### For Azure Blob Storage (Recommended)

Update your `application.properties`:

```properties
# Azure Storage Account Configuration
azure.storage.account.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container.name=assets

# Spring Profile (activate Azure Blob Storage)
spring.profiles.active=azure
```

#### For AWS S3 (Legacy)

Keep the existing configuration and ensure no Azure profile is active:

```properties
# AWS S3 Configuration
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

## Migration Process

### Step 1: Prepare Azure Environment

1. Create Azure Storage Account
2. Create blob container
3. Configure managed identity permissions
4. Test connectivity with Azure CLI: `az storage blob list --account-name yourstorageaccount --container-name assets`

### Step 2: Update Application Configuration

1. Add Azure configuration to `application.properties`
2. Set `spring.profiles.active=azure` to enable Azure Blob Storage
3. Deploy the application

### Step 3: Data Migration (Optional)

If you need to migrate existing data from S3 to Azure Blob Storage:

1. Use Azure Data Factory
2. Use AzCopy tool
3. Use custom migration scripts

### Step 4: Testing

1. Upload new images through the web interface
2. Verify images are stored in Azure Blob Storage
3. Test thumbnail generation
4. Verify image viewing and deletion

## Key Differences

### Authentication
- **S3**: Uses access key/secret key authentication
- **Azure**: Uses Azure Managed Identity (more secure, no credentials in config)

### URLs
- **S3**: Objects accessed via S3 URLs
- **Azure**: Objects accessed via Azure Blob Storage URLs

### Container vs Bucket
- **S3**: Uses buckets
- **Azure**: Uses containers

## Troubleshooting

### Common Issues

1. **Authentication Errors**
   - Ensure managed identity has Storage Blob Data Contributor role
   - For local development, run `az login` or set up service principal

2. **Container Not Found**
   - Verify container name in configuration
   - Ensure container exists in the storage account

3. **Permission Errors**
   - Check Azure RBAC permissions
   - Verify storage account access policies

### Debugging

Enable Azure SDK logging by adding to `application.properties`:

```properties
logging.level.com.azure=DEBUG
```

## Performance Considerations

- Azure Blob Storage offers similar performance to S3
- Consider using Azure CDN for better global performance
- Use appropriate storage tiers (Hot, Cool, Archive) based on access patterns

## Security Best Practices

1. Use Managed Identity instead of connection strings
2. Configure storage account firewall rules
3. Enable audit logging
4. Use private endpoints for enhanced security
5. Regularly rotate any shared access signatures if used

## Monitoring

- Monitor storage metrics in Azure Portal
- Set up alerts for storage capacity and requests
- Use Application Insights for application performance monitoring