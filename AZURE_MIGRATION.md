# Azure Storage Account Migration

This document describes the migration from AWS S3 to Azure Storage Account implemented in this project.

## Overview

The project now supports both AWS S3 and Azure Storage Account as storage backends through a profile-based configuration system. This allows for:

- **Backward Compatibility**: Existing S3 deployments continue to work
- **Gradual Migration**: Teams can migrate at their own pace  
- **Development Flexibility**: Local storage for development, cloud storage for production

## Architecture

### Storage Services

The project implements the `StorageService` interface with multiple implementations:

- **LocalFileStorageService** (`dev` profile): For local development
- **AwsS3Service** (`!dev` profile): For AWS S3 production deployments  
- **AzureStorageService** (`azure` profile): For Azure Storage Account deployments

### Worker Services

Similarly, the worker module has file processing implementations:

- **LocalFileProcessingService** (`dev` profile): Processes files from local storage
- **S3FileProcessingService** (`!dev` profile): Processes files from AWS S3
- **AzureBlobFileProcessingService** (`azure` profile): Processes files from Azure Storage

## Configuration

### Azure Storage Configuration

Add the following properties to your `application.properties`:

```properties
# Azure Storage Configuration
azure.storage.account-name=your-storage-account-name
azure.storage.container-name=your-container-name
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
```

### Profile Activation

Activate the Azure profile in your deployment:

```bash
# For Azure Storage
java -jar app.jar --spring.profiles.active=azure

# For AWS S3 (existing behavior)
java -jar app.jar --spring.profiles.active=prod

# For Local Development (existing behavior)  
java -jar app.jar --spring.profiles.active=dev
```

## Authentication

The Azure Storage implementation uses `DefaultAzureCredential` which supports multiple authentication methods:

1. **Environment Variables**: `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`
2. **Managed Identity**: For Azure VMs, App Service, Function Apps
3. **Azure CLI**: For local development
4. **Visual Studio**: For local development
5. **IntelliJ**: For local development

For production deployments, use Managed Identity when possible.

## Database Schema

The `ImageMetadata` entity has been extended to support both storage types:

```java
public class ImageMetadata {
    // ... existing fields ...
    
    // AWS S3 fields (legacy)
    private String s3Key;
    private String s3Url;
    
    // Azure Storage fields (new)
    private String azureBlobName;
    private String azureBlobUrl;
    
    // ... other fields ...
}
```

## Migration Process

### Option 1: Blue-Green Deployment

1. Deploy new version with Azure profile alongside existing S3 deployment
2. Gradually move traffic to Azure deployment
3. Migrate existing files from S3 to Azure Storage (separate process)
4. Decommission S3 deployment

### Option 2: In-Place Migration

1. Update configuration to include Azure Storage properties
2. Change profile from `prod` to `azure`
3. Restart application
4. Migrate existing files from S3 to Azure Storage

## Dependencies

The migration adds the following dependencies:

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

## Key Differences

| Feature | AWS S3 | Azure Storage |
|---------|--------|---------------|
| Authentication | Access Key + Secret | DefaultAzureCredential |
| Container Concept | Bucket | Container |
| Object Reference | S3 Key | Blob Name |
| URL Generation | S3 GetUrl | Blob URL |
| Storage Type | "s3" | "azure" |

## Testing

The project includes tests for both storage implementations:

```bash
# Run all tests
./mvnw test

# Run Azure-specific tests
./mvnw test -Dtest=AzureStorageServiceTest

# Run profile-based tests  
./mvnw test -Dtest=StorageServiceProfileTest
```

## Troubleshooting

### Authentication Issues

- Ensure proper Azure credentials are configured
- For local development, run `az login` to authenticate with Azure CLI
- For production, ensure Managed Identity is properly configured

### Container Not Found

- Verify container name in configuration
- Ensure the container exists in the storage account
- Check that the application has proper permissions

### Profile Issues

- Verify the correct profile is active
- Check that profile-specific beans are being loaded
- Use Spring Boot Actuator endpoints to verify bean creation

## Performance Considerations

- Azure Storage supports similar performance characteristics to S3
- Consider using Azure CDN for improved global performance  
- Monitor blob storage metrics in Azure Portal
- Implement retry policies for transient failures

## Security

- Use Managed Identity in production environments
- Implement proper access controls at the container level
- Consider using Azure Private Endpoints for enhanced security
- Regular rotation of access keys if using connection strings