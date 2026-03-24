# Modernization Summary: 002-transform-storage-s3-to-blob

## Task
Migrate all file storage operations from AWS S3 to Azure Blob Storage in both web and worker modules.

## Changes Made

### Dependency Updates

**`pom.xml` (parent)**
- Upgraded Lombok to `1.18.30` to ensure compatibility with JDK 21.

**`web/pom.xml` and `worker/pom.xml`**
- Removed `software.amazon.awssdk:s3` (version 2.25.13) dependency.
- Added `com.azure:azure-storage-blob:12.25.4` dependency.
- Added `com.azure:azure-identity:1.13.3` dependency (for Managed Identity / `DefaultAzureCredential`).
- Updated `dependencyManagement` block accordingly.

### New Files

| File | Description |
|------|-------------|
| `web/src/main/java/.../config/AzureBlobConfig.java` | Spring `@Configuration` that creates a `BlobServiceClient` bean using `DefaultAzureCredentialBuilder` (Managed Identity). |
| `web/src/main/java/.../service/AzureBlobService.java` | Replaces `AwsS3Service`. Implements `StorageService` using Azure Blob Storage API for list, upload, download, and delete operations. |
| `worker/src/main/java/.../config/AzureBlobConfig.java` | Same pattern as the web config — `BlobServiceClient` bean with `DefaultAzureCredential`. |
| `worker/src/main/java/.../worker/service/BlobFileProcessingService.java` | Replaces `S3FileProcessingService`. Implements `FileProcessor` using Azure Blob Storage for downloading originals and uploading generated thumbnails. |

### Deleted Files

| File | Reason |
|------|--------|
| `web/src/main/java/.../config/AwsS3Config.java` | Replaced by `AzureBlobConfig.java` |
| `web/src/main/java/.../service/AwsS3Service.java` | Replaced by `AzureBlobService.java` |
| `worker/src/main/java/.../config/AwsS3Config.java` | Replaced by `AzureBlobConfig.java` |
| `worker/src/main/java/.../worker/service/S3FileProcessingService.java` | Replaced by `BlobFileProcessingService.java` |

### Configuration Updates

**`web/src/main/resources/application.properties`**
- Removed: `aws.accessKey`, `aws.secretKey`, `aws.region`, `aws.s3.bucket`
- Added: `azure.storage.blob.endpoint`, `azure.storage.blob.container-name`

**`worker/src/main/resources/application.properties`**
- Removed: `aws.accessKeyId`, `aws.secretKey`, `aws.region`, `aws.s3.bucket`
- Added: `azure.storage.blob.endpoint`, `azure.storage.blob.container-name`

**`web/src/test/resources/application.properties`**
- Replaced dummy AWS credentials with Azure Blob test placeholder properties.

## Authentication

Both modules now use `DefaultAzureCredential` from `azure-identity`, which supports:
- **Azure Managed Identity** (production on Azure)
- Environment variables / service principal (local development / CI)

No static credentials are stored in application properties.

## Key Design Decisions

- `storageType` returned by the new service implementations is `"blob"` (was `"s3"`). This ensures the worker processes only messages from the blob-based web service.
- The `ImageMetadata` entity fields `s3Key` and `s3Url` are left unchanged to avoid breaking the existing database schema; they now hold Azure Blob keys and URLs.
- The `S3StorageItem` DTO and `StorageService` interface are unchanged — Azure Blob operations map cleanly to the existing interface contract.

## Success Criteria

| Criterion | Status |
|-----------|--------|
| passBuild | ✅ PASS |
| passUnitTests | ✅ PASS (1/1 test) |
| generateNewUnitTests | N/A (not required) |
| generateNewIntegrationTests | N/A (not required) |
| passIntegrationTests | N/A (not required) |
| securityComplianceCheck | N/A (not required) |
