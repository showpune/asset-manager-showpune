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
