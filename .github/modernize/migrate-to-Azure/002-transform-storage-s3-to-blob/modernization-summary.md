# Modernization Summary: 002-transform-storage-s3-to-blob

## Task Description
Migrated all file storage operations from AWS S3 to Azure Blob Storage in both the `web` and `worker` modules. Replaced the AWS SDK with the Azure Storage Blobs SDK and configured Managed Identity (DefaultAzureCredential) for credential-free authentication.

## Changes Made

### Dependency Updates

**`pom.xml` (root)**
- Added `lombok.version=1.18.34` to fix pre-existing Lombok + JDK 21 compilation incompatibility.

**`web/pom.xml`**
- Removed `software.amazon.awssdk:s3` dependency.
- Added `com.azure:azure-storage-blob:12.25.0` and `com.azure:azure-identity:1.11.4`.

**`worker/pom.xml`**
- Removed `software.amazon.awssdk:s3` dependency.
- Added `com.azure:azure-storage-blob:12.25.0` and `com.azure:azure-identity:1.11.4`.

### Web Module

| File | Change |
|------|--------|
| `config/AwsS3Config.java` | Deleted |
| `service/AwsS3Service.java` | Deleted |
| `config/AzureBlobStorageConfig.java` | Created — provides a `BlobServiceClient` bean using `DefaultAzureCredentialBuilder` (Managed Identity) |
| `service/AzureBlobStorageService.java` | Created — implements `StorageService` with Azure Blob Storage operations (list, upload, download, delete) |
| `resources/application.properties` | Replaced AWS S3 properties (`aws.*`) with `azure.storage.account-name` and `azure.storage.container-name` |
| `test/resources/application.properties` | Replaced dummy AWS credentials with dummy Azure storage properties |

### Worker Module

| File | Change |
|------|--------|
| `config/AwsS3Config.java` | Deleted |
| `service/S3FileProcessingService.java` | Deleted |
| `config/AzureBlobStorageConfig.java` | Created — provides a `BlobServiceClient` bean using `DefaultAzureCredentialBuilder` (Managed Identity) |
| `service/AzureBlobFileProcessingService.java` | Created — extends `AbstractFileProcessingService`, downloads originals and uploads thumbnails via Azure Blob SDK |
| `resources/application.properties` | Replaced AWS S3 properties with `azure.storage.account-name` and `azure.storage.container-name` |

### Key Implementation Details

- **Authentication**: `DefaultAzureCredentialBuilder` is used in both modules, enabling Managed Identity in Azure and falling back to environment variables / developer tools locally.
- **Storage type**: The `getStorageType()` method returns `"azure-blob"` in both services, ensuring the web module and worker module use consistent storage type matching for message routing.
- **API mapping**:
  - List: `BlobContainerClient.listBlobs()` → `PagedIterable<BlobItem>`
  - Upload: `BlobClient.upload(InputStream, long, boolean overwrite)`
  - Download (stream): `BlobClient.openInputStream()`
  - Download (file): `BlobClient.downloadToFile(String path, boolean overwrite)`
  - Upload from file: `BlobClient.uploadFromFile(String path, boolean overwrite)`
  - Delete: `BlobClient.delete()`
  - URL: `BlobClient.getBlobUrl()`

## Success Criteria Results

| Criterion | Result |
|-----------|--------|
| passBuild | ✅ PASS |
| passUnitTests | ✅ PASS (1 test, 0 failures) |
| generateNewUnitTests | N/A (not required) |
| generateNewIntegrationTests | N/A (not required) |
| passIntegrationTests | N/A (not required) |
| securityComplianceCheck | N/A (not required) |
