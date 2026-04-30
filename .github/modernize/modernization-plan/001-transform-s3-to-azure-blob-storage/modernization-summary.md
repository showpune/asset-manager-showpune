# Modernization Summary: 001-transform-s3-to-azure-blob-storage

## Overview

Migrated both `web` and `worker` modules from AWS S3 SDK to Azure Blob Storage SDK, using `DefaultAzureCredential` (Managed Identity) for authentication. The existing adaptor interface pattern was preserved throughout.

## Changes Made

### New Files
- `web/src/main/java/.../config/AzureBlobStorageConfig.java` — Configures `BlobServiceClient` using `DefaultAzureCredentialBuilder` with `@Profile("!baseline")`.
- `web/src/main/java/.../service/AzureBlobStorageService.java` — Implements `StorageService` with Azure Blob Storage; handles list, upload (with content-type headers), streaming download via `openInputStream()`, delete, and URL generation. Uses `ServiceBusTemplate` for message dispatch.
- `worker/src/main/java/.../config/AzureBlobStorageConfig.java` — Worker-side `BlobServiceClient` config with `DefaultAzureCredential`.
- `worker/src/main/java/.../service/AzureBlobStorageFileProcessingService.java` — Extends `AbstractFileProcessingService`; implements `downloadOriginal`, `uploadThumbnail` (with `BlobHttpHeaders` for content type), and thumbnail metadata persistence via `findByS3Key()`.

### Modified Files
- `web/pom.xml` — Added `azure-sdk-bom`, `azure-storage-blob`, `azure-storage-blob-batch`, `azure-identity` dependencies.
- `worker/pom.xml` — Same Azure dependencies as web.
- `web/src/main/resources/application.properties` — Replaced `aws.*` properties with `azure.blob.endpoint` and `azure.blob.container.name`.
- `worker/src/main/resources/application.properties` — Same replacement as web.
- `web/src/test/resources/application.properties` — Replaced AWS dummy credentials with Azure Blob test placeholders.
- `web/src/main/java/.../AssetsManagerApplication.java` — Removed `@EnableRabbit` annotation (superseded by Service Bus migration).
- `web/src/main/java/.../service/LocalFileStorageService.java` — Replaced `RabbitTemplate` with `ServiceBusTemplate` for dev-profile message dispatch.
- `web/src/test/java/.../AssetsManagerApplicationTests.java` — Added `@MockBean` for `ServiceBusTemplate` and `ServiceBusAdministrationClient` to allow context loading in tests.
- `web/src/main/java/.../repository/ImageMetadataRepository.java` — Added `findByS3Key(String s3Key)` derived query method.
- `worker/src/main/java/.../repository/ImageMetadataRepository.java` — Added `findByS3Key(String s3Key)` derived query method.

### Deleted Files
- `web/src/main/java/.../config/AwsS3Config.java`
- `web/src/main/java/.../service/AwsS3Service.java`
- `worker/src/main/java/.../config/AwsS3Config.java`
- `worker/src/main/java/.../service/S3FileProcessingService.java`

## Key Design Decisions

- **Managed Identity**: `DefaultAzureCredential` used for both web and worker; no static credentials.
- **Private container + server-side proxy URLs**: `generateUrl()` returns `/s3/view/{key}` in both web and worker services (consistent with `LocalFileStorageService`). Raw blob URLs are inaccessible from browsers for private containers; the web app's `S3Controller.viewObject()` acts as the authenticated proxy.
- **Content-type headers**: `BlobHttpHeaders` set on every upload to prevent `application/octet-stream` defaults.
- **Streaming download**: `BlobClient.openInputStream()` used in `getObject()` for true non-buffering streaming (avoids loading entire blobs into memory).
- **N+1 prevention**: `listObjects()` pre-loads all metadata into a `Map` before streaming blobs.
- **findByS3Key**: Metadata lookup uses `findByS3Key()` (not `findById()`) since the DB primary key is a UUID, not the blob key.
- **Profile strategy**: Config beans use `@Profile("!baseline")`; service implementations use `@Profile("!dev & !baseline")`.

## Consistency Check Fixes

Issues identified by the consistency check and resolved:

| Severity | Issue | Fix |
|----------|-------|-----|
| Major | Missing content-type on upload | Added `BlobHttpHeaders` + `BlobParallelUploadOptions` |
| Major | In-memory download buffering | Replaced `downloadContent().toStream()` with `openInputStream()` |
| Major | Worker `generateUrl` inconsistency | Changed to return `/s3/view/{key}` (consistent with web) |
| Major | Thumbnail metadata lookup by wrong ID type | Changed `findById()` to `findByS3Key()` |
| Minor | Unnecessary try-catch in `deleteObject()` | Removed redundant catch block |

## Success Criteria

- ✅ Build passes (`mvn clean test -pl web,worker`)
- ✅ All unit tests pass (web: 1 test, worker: 0 tests)
- ✅ No Critical or Major consistency issues remaining
