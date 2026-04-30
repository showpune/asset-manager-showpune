package com.microsoft.migration.assets.worker.postmigration;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import com.microsoft.migration.assets.worker.service.AzureBlobStorageFileProcessingService;
import com.microsoft.migration.assets.worker.util.StorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Post-migration integration tests for {@link AzureBlobStorageFileProcessingService}.
 *
 * <p>Verifies Azure Blob Storage SDK interactions at the SDK boundary via Mockito mocks.
 * All fixture data is loaded via Layer 2 post-migration manifest entries.
 *
 * <p>POST-MIGRATION ONLY — added in Phase 3.
 */
@ExtendWith(MockitoExtension.class)
class AzureBlobFileProcessingPostMigrationIT {

    @Mock
    private BlobServiceClient blobServiceClient;
    @Mock
    private BlobContainerClient containerClient;
    @Mock
    private BlobClient blobClient;
    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @TempDir
    Path tempDir;

    private AzureBlobStorageFileProcessingService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AzureBlobStorageFileProcessingService(blobServiceClient, imageMetadataRepository);

        Field containerNameField = AzureBlobStorageFileProcessingService.class.getDeclaredField("containerName");
        containerNameField.setAccessible(true);
        containerNameField.set(service, "test-container");

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        Field retryField = com.microsoft.migration.assets.worker.service.AbstractFileProcessingService.class
                .getDeclaredField("retryTemplate");
        retryField.setAccessible(true);
        retryField.set(service, retryTemplate);

        lenient().when(blobServiceClient.getBlobContainerClient("test-container")).thenReturn(containerClient);
        lenient().when(containerClient.getBlobClient(any())).thenReturn(blobClient);
    }

    // ----- storage type -----

    @Test
    void storageType_isAzureBlob() throws Exception {
        assertThat(service.getStorageType())
                .isEqualTo(PostMigrationFixtures.getExpectedStorageType("azure-upload-small-jpg"));
    }

    // ----- downloadOriginal -----

    @Test
    void downloadOriginal_callsBlobDownloadToFile() throws Exception {
        Path destination = tempDir.resolve("original.jpg");

        service.downloadOriginal("test-key.jpg", destination);

        verify(blobClient).downloadToFile(destination.toString(), true);
    }

    // ----- uploadThumbnail -----

    @Test
    void uploadThumbnail_uploadsWithCorrectContentTypeHeader() throws Exception {
        String expectedContentType = PostMigrationFixtures.getExpectedContentType("azure-upload-small-jpg");
        Path thumbnailPath = tempDir.resolve("thumbnail.jpg");
        byte[] bytes = PostMigrationFixtures.loadPayloadBytes("azure-upload-small-jpg");
        Files.write(thumbnailPath, bytes);

        when(imageMetadataRepository.findByS3Key(anyString())).thenReturn(Optional.empty());
        when(imageMetadataRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.uploadThumbnail(thumbnailPath, "test-key_thumbnail.jpg", expectedContentType);

        ArgumentCaptor<BlobParallelUploadOptions> captor =
                ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(captor.capture(), any(), any());
        BlobHttpHeaders headers = captor.getValue().getHeaders();
        assertThat(headers).isNotNull();
        assertThat(headers.getContentType()).isEqualTo(expectedContentType);
    }

    @Test
    void uploadThumbnail_createsNewMetadataWhenNotExists() throws Exception {
        Path thumbnailPath = tempDir.resolve("thumb.jpg");
        Files.write(thumbnailPath, PostMigrationFixtures.loadPayloadBytes("azure-upload-small-jpg"));
        String originalKey = "test-key.jpg";
        String thumbnailKey = StorageUtil.getThumbnailKey(originalKey);  // test-key_thumbnail.jpg

        when(imageMetadataRepository.findByS3Key(originalKey)).thenReturn(Optional.empty());
        ArgumentCaptor<ImageMetadata> metaCaptor = ArgumentCaptor.forClass(ImageMetadata.class);
        when(imageMetadataRepository.save(metaCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.uploadThumbnail(thumbnailPath, thumbnailKey, "image/jpeg");

        ImageMetadata saved = metaCaptor.getValue();
        assertThat(saved.getS3Key()).isEqualTo(originalKey);
        assertThat(saved.getThumbnailKey()).isEqualTo(thumbnailKey);
        // URL must start with /s3/view/
        assertThat(saved.getThumbnailUrl()).startsWith(
                PostMigrationFixtures.getExpectedUrlPrefix("azure-upload-small-jpg"));
    }

    @Test
    void uploadThumbnail_updatesExistingMetadataRecord() throws Exception {
        Path thumbnailPath = tempDir.resolve("thumb2.jpg");
        Files.write(thumbnailPath, PostMigrationFixtures.loadPayloadBytes("azure-upload-medium-png"));
        String originalKey = "existing-key.png";
        String thumbnailKey = StorageUtil.getThumbnailKey(originalKey);  // existing-key_thumbnail.png

        ImageMetadata existing = new ImageMetadata();
        existing.setId("existing-id");
        existing.setS3Key(originalKey);
        existing.setFilename("existing.png");
        when(imageMetadataRepository.findByS3Key(originalKey)).thenReturn(Optional.of(existing));
        ArgumentCaptor<ImageMetadata> metaCaptor = ArgumentCaptor.forClass(ImageMetadata.class);
        when(imageMetadataRepository.save(metaCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.uploadThumbnail(thumbnailPath, thumbnailKey, "image/png");

        ImageMetadata saved = metaCaptor.getValue();
        // Must update existing record, not create a new one
        assertThat(saved.getId()).isEqualTo("existing-id");
        assertThat(saved.getFilename()).isEqualTo("existing.png");
        assertThat(saved.getThumbnailKey()).isEqualTo(thumbnailKey);
        assertThat(saved.getThumbnailUrl()).startsWith(
                PostMigrationFixtures.getExpectedUrlPrefix("azure-upload-medium-png"));
    }

    // ----- URL format -----

    @Test
    void generateUrl_returnsCorrectPrefix() throws Exception {
        Path thumbnailPath = tempDir.resolve("url-test.jpg");
        Files.write(thumbnailPath, PostMigrationFixtures.loadPayloadBytes("azure-upload-small-jpg"));
        when(imageMetadataRepository.findByS3Key(any())).thenReturn(Optional.empty());
        ArgumentCaptor<ImageMetadata> metaCaptor = ArgumentCaptor.forClass(ImageMetadata.class);
        when(imageMetadataRepository.save(metaCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.uploadThumbnail(thumbnailPath, "test-url-key_thumbnail.jpg", "image/jpeg");

        String expectedPrefix = PostMigrationFixtures.getExpectedUrlPrefix("azure-upload-small-jpg");
        assertThat(metaCaptor.getValue().getThumbnailUrl()).startsWith(expectedPrefix);
    }
}
