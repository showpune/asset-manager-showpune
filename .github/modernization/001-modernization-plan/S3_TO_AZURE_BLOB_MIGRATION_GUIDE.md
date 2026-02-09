# AWS S3 to Azure Blob Storage Migration Guide

## Overview
This document provides a comprehensive guide for the migration from AWS S3 to Azure Blob Storage completed as part of the modernization initiative. The migration affects both the web and worker modules of the asset-manager application.

## Migration Summary

### Date Completed
February 9, 2026

### Task ID
002-transform-s3-to-azure-blob

### Scope
- **Web Module**: Complete migration of storage service from AWS S3 to Azure Blob Storage
- **Worker Module**: Complete migration of file processing service from AWS S3 to Azure Blob Storage

## Changes Made

### 1. Dependency Updates

#### Web Module (`web/pom.xml`)
**Removed:**
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

**Added:**
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

#### Worker Module (`worker/pom.xml`)
Same dependency changes as the web module.

### 2. Configuration Changes

#### Web Module Configuration
**File:** `web/src/main/java/com/microsoft/migration/assets/config/AzureBlobStorageConfig.java`

**Old Class:** `AwsS3Config.java` (deleted)

**New Configuration:**
- Uses `DefaultAzureCredentialBuilder` for managed identity authentication
- No hardcoded credentials required
- Configuration via application properties:
  - `azure.storage.account-name`: Azure Storage Account name
  - `azure.storage.container-name`: Azure Blob Container name

**Authentication Method:**
The migration uses **DefaultAzureCredential** which supports multiple authentication methods in the following order:
1. Environment variables
2. Managed Identity (for Azure-hosted apps)
3. Azure CLI credentials
4. IntelliJ/Visual Studio Code credentials

This eliminates the need for hardcoded access keys and secrets.

#### Worker Module Configuration
**File:** `worker/src/main/java/com/microsoft/migration/assets/worker/config/AzureBlobStorageConfig.java`

Same configuration approach as the web module.

### 3. Service Implementation Changes

#### Web Module Service
**Old:** `AwsS3Service.java` → **New:** `AzureBlobStorageService.java`

**Key Changes:**
- `S3Client` replaced with `BlobServiceClient`
- `ListObjectsV2Request` replaced with `containerClient.listBlobs()`
- `PutObjectRequest` replaced with `blobClient.upload()` and `blobClient.setHttpHeaders()`
- `GetObjectRequest` replaced with `blobClient.openInputStream()`
- `DeleteObjectRequest` replaced with `blobClient.deleteIfExists()`
- URL generation now uses `blobClient.getBlobUrl()`
- Storage type identifier changed from `"s3"` to `"azure-blob"`

#### Worker Module Service
**Old:** `S3FileProcessingService.java` → **New:** `AzureBlobFileProcessingService.java`

**Key Changes:**
- `S3Client` replaced with `BlobServiceClient`
- `GetObjectRequest` for downloads replaced with `blobClient.openInputStream()`
- `PutObjectRequest` for uploads replaced with `blobClient.uploadFromFile()` and `blobClient.setHttpHeaders()`
- URL generation now uses `blobClient.getBlobUrl()`
- Storage type identifier changed from `"s3"` to `"azure-blob"`

### 4. Model Updates

**Old:** `S3StorageItem.java` → **New:** `BlobStorageItem.java`

The model class was renamed to reflect the new storage backend, but the structure remains the same:
- `key`: Blob name/path
- `name`: Display name
- `size`: File size in bytes
- `lastModified`: Last modification timestamp
- `uploadedAt`: Upload timestamp
- `url`: Public URL to the blob

### 5. Interface Updates

**File:** `StorageService.java`

Updated documentation and return types:
- Return type changed from `List<S3StorageItem>` to `List<BlobStorageItem>`
- Storage type comments updated to reflect "Azure Blob Storage" instead of "AWS S3"

### 6. Application Properties

#### Web Module (`web/src/main/resources/application.properties`)
**Removed:**
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**Added:**
```properties
azure.storage.account-name=your-storage-account-name
azure.storage.container-name=your-container-name
```

#### Worker Module (`worker/src/main/resources/application.properties`)
**Removed:**
```properties
aws.accessKeyId=your-access-key-Id
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**Added:**
```properties
azure.storage.account-name=your-storage-account-name
azure.storage.container-name=your-container-name
```

#### Test Properties (`web/src/test/resources/application.properties`)
**Removed:**
```properties
aws.accessKey=test-access-key
aws.secretKey=test-secret-key
aws.region=us-east-1
aws.s3.bucket=test-bucket
```

**Added:**
```properties
azure.storage.account-name=test-storage-account
azure.storage.container-name=test-container
```

## Deployment Configuration

### Prerequisites
1. **Azure Storage Account**: Create an Azure Storage Account in your target subscription
2. **Blob Container**: Create a container within the storage account
3. **Authentication Setup**: Configure managed identity or service principal for the application

### Environment Configuration

#### Option 1: Managed Identity (Recommended for Azure-hosted apps)
1. Enable System-assigned or User-assigned Managed Identity on your Azure App Service or Container App
2. Grant the managed identity the following roles on the Storage Account:
   - `Storage Blob Data Contributor` - For read/write operations
   - `Storage Blob Data Reader` - For read-only operations (if applicable)

#### Option 2: Service Principal
1. Create a Service Principal in Azure AD
2. Grant the Service Principal appropriate roles on the Storage Account
3. Set environment variables:
   ```bash
   AZURE_CLIENT_ID=<service-principal-client-id>
   AZURE_CLIENT_SECRET=<service-principal-secret>
   AZURE_TENANT_ID=<azure-tenant-id>
   ```

#### Option 3: Connection String (Not Recommended for Production)
If using connection strings, modify the configuration to use:
```java
new BlobServiceClientBuilder()
    .connectionString(connectionString)
    .buildClient();
```

### Application Configuration
Update the following properties in your deployment environment:
```properties
azure.storage.account-name=<your-storage-account-name>
azure.storage.container-name=<your-container-name>
```

## API Mapping

### Upload Operations
| AWS S3 | Azure Blob Storage |
|--------|-------------------|
| `PutObjectRequest.builder()` | `blobClient.upload()` |
| `.bucket(bucketName)` | Handled by container client |
| `.key(key)` | Handled by blob client |
| `.contentType(contentType)` | `blobClient.setHttpHeaders()` |
| `s3Client.putObject(request, body)` | `blobClient.upload(inputStream, length, overwrite)` |

### Download Operations
| AWS S3 | Azure Blob Storage |
|--------|-------------------|
| `GetObjectRequest.builder()` | `blobClient.openInputStream()` |
| `.bucket(bucketName).key(key)` | Handled by blob client |
| `s3Client.getObject(request)` | Direct stream access |

### List Operations
| AWS S3 | Azure Blob Storage |
|--------|-------------------|
| `ListObjectsV2Request.builder()` | `containerClient.listBlobs()` |
| `.bucket(bucketName)` | Handled by container client |
| `s3Client.listObjectsV2(request)` | Direct iteration |
| `.contents()` | Stream of `BlobItem` |

### Delete Operations
| AWS S3 | Azure Blob Storage |
|--------|-------------------|
| `DeleteObjectRequest.builder()` | `blobClient.deleteIfExists()` |
| `.bucket(bucketName).key(key)` | Handled by blob client |
| `s3Client.deleteObject(request)` | Single method call |

### URL Generation
| AWS S3 | Azure Blob Storage |
|--------|-------------------|
| `GetUrlRequest.builder()` | `blobClient.getBlobUrl()` |
| `s3Client.utilities().getUrl(request)` | Direct method call |

## Testing

### Build Verification
```bash
./mvnw clean compile
```
**Result:** ✅ BUILD SUCCESS

### Unit Tests
```bash
./mvnw test
```
**Result:** ✅ All tests passed (1 test executed)

### Integration Testing
To test the migration in your environment:

1. **Create Azure Resources:**
   ```bash
   # Create resource group
   az group create --name myResourceGroup --location eastus
   
   # Create storage account
   az storage account create \
     --name mystorageaccount \
     --resource-group myResourceGroup \
     --location eastus \
     --sku Standard_LRS
   
   # Create blob container
   az storage container create \
     --name mycontainer \
     --account-name mystorageaccount
   ```

2. **Configure Application:**
   Update `application.properties`:
   ```properties
   azure.storage.account-name=mystorageaccount
   azure.storage.container-name=mycontainer
   ```

3. **Run Application:**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Test Operations:**
   - Upload a file through the web interface
   - Verify the file appears in Azure Blob Storage
   - Verify thumbnail generation by the worker
   - Download and delete operations

## Migration Checklist

- [x] Update Maven dependencies (web and worker modules)
- [x] Create Azure Blob Storage configuration classes
- [x] Implement Azure Blob Storage service for web module
- [x] Implement Azure Blob Storage file processing service for worker module
- [x] Update model classes (rename S3StorageItem to BlobStorageItem)
- [x] Update service interfaces
- [x] Update application properties
- [x] Update test configuration
- [x] Verify build compilation
- [x] Run and pass unit tests
- [x] Update controller to use new model class
- [x] Verify storage type identifiers

## Breaking Changes

### Configuration
- **Property Names Changed:**
  - `aws.accessKey` → Removed (using managed identity)
  - `aws.secretKey` → Removed (using managed identity)
  - `aws.region` → Removed (derived from storage account)
  - `aws.s3.bucket` → `azure.storage.container-name`
  - New: `azure.storage.account-name`

### Storage Type Identifier
- Changed from `"s3"` to `"azure-blob"`
- This affects message routing in RabbitMQ between web and worker modules
- Both modules must use the same storage type identifier

### Authentication
- No longer using access key/secret key authentication
- Now using Azure managed identity (DefaultAzureCredential)
- Requires proper Azure RBAC configuration

## Rollback Plan

If rollback is necessary:

1. **Revert Code Changes:**
   ```bash
   git revert <migration-commit-hash>
   ```

2. **Restore AWS S3 Dependencies:**
   - Restore original `pom.xml` files
   - Restore `AwsS3Config.java` classes
   - Restore `AwsS3Service.java` and `S3FileProcessingService.java`

3. **Restore Configuration:**
   - Restore AWS credentials in `application.properties`
   - Remove Azure configuration properties

4. **Rebuild and Redeploy:**
   ```bash
   ./mvnw clean install
   ```

## Security Improvements

### Before (AWS S3)
- Used access keys and secret keys stored in configuration
- Credentials could be exposed in logs or configuration files
- Manual key rotation required

### After (Azure Blob Storage)
- Uses managed identity (no credentials in code)
- Automatic credential rotation by Azure
- RBAC-based access control
- Audit logging via Azure Monitor

## Performance Considerations

### Similarities
- Both services provide similar performance for object storage
- Both support parallel uploads/downloads
- Both offer CDN integration options

### Differences
- Azure Blob Storage tiers (Hot, Cool, Archive) for cost optimization
- Different pricing models (pay-as-you-go)
- Azure offers lifecycle management policies

## Monitoring and Observability

### Azure Monitor Integration
Monitor your blob storage operations:

1. **Enable Diagnostic Settings:**
   ```bash
   az monitor diagnostic-settings create \
     --resource /subscriptions/{subscription-id}/resourceGroups/{resource-group}/providers/Microsoft.Storage/storageAccounts/{storage-account} \
     --name myDiagnosticSetting \
     --logs '[{"category": "StorageRead","enabled": true},{"category": "StorageWrite","enabled": true}]' \
     --metrics '[{"category": "Transaction","enabled": true}]'
   ```

2. **Key Metrics to Monitor:**
   - Total requests
   - Latency (E2E and server)
   - Availability
   - Capacity used
   - Egress/Ingress

### Application Insights
The Azure SDK automatically integrates with Application Insights when configured:
```properties
azure.application-insights.instrumentation-key=<your-key>
```

## Cost Optimization

### Recommendations
1. **Use Blob Storage Tiers:**
   - Hot tier: Frequently accessed data
   - Cool tier: Infrequently accessed data (30+ days)
   - Archive tier: Rarely accessed data (180+ days)

2. **Lifecycle Management:**
   Configure policies to automatically move blobs between tiers based on age

3. **Enable Compression:**
   Compress images before upload to reduce storage costs

## Support and Troubleshooting

### Common Issues

#### Issue: "BlobStorageException: Authentication failed"
**Solution:** Verify managed identity is properly configured and has the correct RBAC roles

#### Issue: "Container not found"
**Solution:** Ensure the container exists and the name matches the configuration

#### Issue: "Connection timeout"
**Solution:** Check network connectivity and firewall rules

### Useful Azure CLI Commands

```bash
# List blobs in container
az storage blob list --account-name <account> --container-name <container>

# Upload blob
az storage blob upload --account-name <account> --container-name <container> --file <file> --name <blob-name>

# Download blob
az storage blob download --account-name <account> --container-name <container> --name <blob-name> --file <file>

# Delete blob
az storage blob delete --account-name <account> --container-name <container> --name <blob-name>
```

## References

### Azure Documentation
- [Azure Blob Storage Documentation](https://docs.microsoft.com/azure/storage/blobs/)
- [Azure SDK for Java](https://docs.microsoft.com/java/api/overview/azure/storage-blob-readme)
- [DefaultAzureCredential](https://docs.microsoft.com/java/api/com.azure.identity.defaultazurecredential)

### Code References
- Web Configuration: `web/src/main/java/com/microsoft/migration/assets/config/AzureBlobStorageConfig.java`
- Web Service: `web/src/main/java/com/microsoft/migration/assets/service/AzureBlobStorageService.java`
- Worker Configuration: `worker/src/main/java/com/microsoft/migration/assets/worker/config/AzureBlobStorageConfig.java`
- Worker Service: `worker/src/main/java/com/microsoft/migration/assets/worker/service/AzureBlobFileProcessingService.java`

## Conclusion

The migration from AWS S3 to Azure Blob Storage has been successfully completed with:
- ✅ All code changes implemented
- ✅ Build passing
- ✅ Unit tests passing
- ✅ Improved security through managed identity
- ✅ Comprehensive documentation

The application is now ready for deployment to Azure with native Azure Blob Storage integration.
