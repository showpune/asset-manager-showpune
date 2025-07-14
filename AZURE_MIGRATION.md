# Azure Storage Migration Guide

This document describes how to migrate from AWS S3 to Azure Storage Account.

## Migration Overview

The application now supports both AWS S3 and Azure Storage Account through a configurable interface. You can switch between storage providers using Spring profiles.

## Configuration

### Using AWS S3 (Default)
By default, the application uses AWS S3. No profile activation is needed.

Configuration in `application.properties`:
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

### Using Azure Storage Account
To use Azure Storage Account, activate the `azure` profile.

#### Method 1: Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=azure
```

#### Method 2: Command Line Argument
```bash
java -jar app.jar --spring.profiles.active=azure
```

#### Method 3: application.properties
```properties
spring.profiles.active=azure
```

### Azure Configuration
When using the `azure` profile, configure these properties:

```properties
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=assets
```

## Authentication

### Azure Storage Account Authentication
The application uses Azure DefaultAzureCredential which supports multiple authentication methods:

1. **Environment Variables**: Set these in your environment:
   ```bash
   export AZURE_CLIENT_ID=your-client-id
   export AZURE_CLIENT_SECRET=your-client-secret
   export AZURE_TENANT_ID=your-tenant-id
   ```

2. **Azure CLI**: Login using Azure CLI:
   ```bash
   az login
   ```

3. **Managed Identity**: When deployed to Azure, managed identity will be used automatically.

4. **Visual Studio**: When running locally with Visual Studio signed in to Azure.

## Key Changes Made

### Dependencies
- Added Azure Storage Blob SDK dependencies
- Added Azure Identity SDK for authentication

### Services
- Created `AzureStorageService` implementing the same `StorageService` interface
- Created `AzureFileProcessingService` for thumbnail processing
- Updated profiles to prevent conflicts between S3 and Azure services

### Configuration
- Added `AzureStorageConfig` for Azure blob service client configuration
- Updated profiles in existing AWS configurations

### UI Changes
- Made templates storage-provider agnostic
- Updated URLs from `/s3` to `/storage`
- Added storage type indicator in the UI

### Model Changes
- Renamed `S3StorageItem` to `StorageItem` for provider independence
- Updated all references throughout the codebase

## Storage Provider Features

Both storage providers support:
- File upload with metadata storage
- File listing and viewing
- File deletion
- Thumbnail generation via RabbitMQ worker
- Drag-and-drop upload interface

## Migration Steps

1. **Setup Azure Storage Account**:
   - Create an Azure Storage Account
   - Create a container named 'assets' (or update configuration)
   - Configure authentication (service principal, managed identity, etc.)

2. **Update Configuration**:
   - Set the Azure storage endpoint in your configuration
   - Ensure authentication is properly configured

3. **Switch Profiles**:
   - Activate the `azure` profile using one of the methods above
   - Restart the application

4. **Verify Migration**:
   - Upload a test file
   - Verify it appears in Azure Storage Account
   - Test all operations (view, delete, thumbnail generation)

## Troubleshooting

### Common Issues

1. **Authentication Error**: Ensure Azure credentials are properly configured
2. **Container Not Found**: Create the container in Azure Storage Account
3. **Profile Not Active**: Verify the `azure` profile is properly activated
4. **Dependencies Missing**: Ensure all Azure dependencies are included in the build

### Checking Active Profile
You can verify which profile is active by checking the application logs or adding this to your configuration:
```properties
logging.level.org.springframework.core.env=DEBUG
```

## Rollback Plan

To rollback to AWS S3:
1. Remove or change the active profile from `azure`
2. Restart the application
3. The application will automatically use AWS S3 configuration

## Performance Considerations

- Azure Storage Account provides similar performance to AWS S3
- Consider using Azure CDN for better global performance
- Monitor costs as Azure pricing structure may differ from AWS