# Coverage Matrix — Baseline Tests

## Web Module (assets-manager-web)

| Operation | Happy Path | Boundary | Special Input | Error/Failure |
|---|---|---|---|---|
| listObjects | `WebBaselineIT#listObjects_afterUpload_returnsItems` | `WebBaselineIT#listObjects_emptyStorage_returnsEmptyList` | — | — |
| uploadObject | `WebBaselineIT#uploadObject_validJpeg_redirectsToList` | `WebBaselineIT#uploadObject_emptyFile_redirectsToUpload` | `WebBaselineIT#uploadObject_unicodeFilename_succeeds` | — |
| getObject | `WebBaselineIT#viewObject_existingKey_returnsContent` | — | — | `WebBaselineIT#viewObject_nonExistentKey_returns404` |
| deleteObject | `WebBaselineIT#deleteObject_existingKey_redirectsToList` | — | — | `WebBaselineIT#deleteObject_nonExistentKey_redirectsToList` |
| uploadObject → message | `WebBaselineIT#uploadObject_recordsProcessingMessage` | — | — | — |

## Worker Module (assets-manager-worker)

| Operation | Happy Path | Boundary | Special Input | Error/Failure |
|---|---|---|---|---|
| process (JPEG) | `WorkerBaselineIT#process_validJpegMessage_generatesThumbnail` | — | — | — |
| process (PNG) | `WorkerBaselineIT#process_validPngMessage_generatesThumbnail` | — | — | — |
| process (wrong storage type) | — | `WorkerBaselineIT#process_wrongStorageType_skipsProcessing` | — | — |
| process (missing file) | — | — | — | `WorkerBaselineIT#process_missingFile_throwsException` |
| process (retry) | — | — | — | `WorkerBaselineIT#process_retryExhausted_recordsFailure` |

## Post-Migration Tests — Web Module (AzureBlobStorageServicePostMigrationIT)

| Operation | SDK-Call / Target Behavior | Assertion Type |
|---|---|---|
| storageType | returns "azure-blob" | Return value |
| uploadObject (JPEG) | BlobHttpHeaders content-type = "image/jpeg" | SDK call param |
| uploadObject (PNG) | BlobHttpHeaders content-type = "image/png" | SDK call param |
| uploadObject → message | ServiceBusTemplate.sendAsync called with correct queue + payload | SDK call param |
| URL format | getObjectUrl returns "/s3/view/{key}" | Return value |
| getObject | BlobClient.openInputStream() invoked, stream returned | SDK call |
| deleteObject | BlobClient.delete() called for both original + thumbnail keys | SDK calls (2x) |
| deleteObject → metadata | ImageMetadataRepository.deleteByStorageKey() called | SDK call |
| listObjects (empty) | Returns empty list when container is empty | Return value |
| listObjects (single) | Returns list with matching DTO | Return value |

## Post-Migration Tests — Worker Module (AzureBlobFileProcessingPostMigrationIT)

| Operation | SDK-Call / Target Behavior | Assertion Type |
|---|---|---|
| storageType | returns "azure-blob" | Return value |
| downloadOriginal | BlobClient.downloadContent() invoked with correct key | SDK call |
| uploadThumbnail (content-type) | BlobParallelUploadOptions headers set with correct MIME type | SDK call param |
| uploadThumbnail (new metadata) | ImageMetadataRepository.save() called with correct thumbnailKey | SDK call param |
| uploadThumbnail (existing metadata) | Existing record updated, thumbnailKey field set | SDK call param |
| generateUrl | URL format uses configured endpoint + container | Return value |

## Post-Migration Tests — Worker Module (ServiceBusListenerPostMigrationIT)

| Operation | SDK-Call / Target Behavior | Assertion Type |
|---|---|---|
| onMessage (success) | ServiceBusReceivedMessageContext.complete() called | SDK call |
| onMessage (failure) | ServiceBusReceivedMessageContext.abandon() called | SDK call |
| onMessage (retry → abandon) | abandon() called after 3 retry attempts | SDK call + retry |
| onMessage (null context) | Processes without NPE when context is null | Exception safety |
