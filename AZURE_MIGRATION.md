# Azure Storage Account Migration Guide

This application now supports both AWS S3 and Azure Storage Account.

## Configuration

### Using AWS S3 (Legacy)
Set the Spring profile to `s3`:
```properties
spring.profiles.active=s3
```

### Using Azure Storage Account (Recommended)
Set the Spring profile to `azure`:
```properties
spring.profiles.active=azure
```

## Environment Configuration

### For AWS S3
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

### For Azure Storage Account
```properties
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

## Authentication

### AWS S3
Uses AWS access key and secret key for authentication.

### Azure Storage Account
Uses Azure Default Credentials (recommended):
- Managed Identity (when running in Azure)
- Azure CLI credentials (for local development)
- Environment variables (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID)
- Visual Studio Code Azure Account extension

## Migration Steps

1. Create an Azure Storage Account
2. Create a container in the Storage Account
3. Set up authentication (Managed Identity or service principal)
4. Update configuration to use `azure` profile
5. Update environment variables with Azure Storage endpoint and container name
6. Deploy and test

## Features Supported

Both S3 and Azure Storage implementations support:
- File upload and download
- File listing
- File deletion
- Thumbnail generation
- Metadata storage in PostgreSQL database

## API Compatibility

The REST API remains unchanged - the storage backend is transparent to API consumers.