# Azure Storage Migration Guide

This project has been migrated to support Azure Storage Account alongside the existing AWS S3 implementation.

## Running with Azure Storage

To run the application with Azure Storage Account:

1. **Configure your Azure Storage Account:**
   - Create an Azure Storage Account
   - Create a container (e.g., "images")
   - Ensure your application has the necessary Azure credentials configured

2. **Update configuration:**
   - Copy `application-azure.properties` to your deployment environment
   - Update the `azure.storage.endpoint` with your storage account endpoint
   - Update the `azure.storage.container` with your container name

3. **Run with Azure profile:**
   ```bash
   # For web module
   java -jar web/target/assets-manager-web-0.0.1-SNAPSHOT.jar --spring.profiles.active=azure

   # For worker module  
   java -jar worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar --spring.profiles.active=azure
   ```

## Running with AWS S3 (Legacy)

To continue using AWS S3:

1. **Use the AWS profile:**
   ```bash
   # For web module
   java -jar web/target/assets-manager-web-0.0.1-SNAPSHOT.jar --spring.profiles.active=aws

   # For worker module
   java -jar worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar --spring.profiles.active=aws
   ```

## Azure Authentication

The Azure Storage implementation uses `DefaultAzureCredential` which supports multiple authentication methods:

1. **Environment Variables:** Set `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`
2. **Managed Identity:** Works automatically when deployed to Azure services
3. **Azure CLI:** Use `az login` for local development
4. **IntelliJ/Eclipse/VS Code:** Use built-in Azure authentication

## Configuration Properties

### Azure Storage Properties
```properties
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=images
```

### AWS S3 Properties (Legacy)
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

## Implementation Details

- **AzureStorageService**: Implements the `StorageService` interface for Azure Blob Storage
- **AzureBlobProcessingService**: Handles thumbnail generation for Azure blobs
- **Profiles**: Use `azure` for Azure Storage, `aws` for AWS S3, `dev` for local file storage

## Dependencies Added

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