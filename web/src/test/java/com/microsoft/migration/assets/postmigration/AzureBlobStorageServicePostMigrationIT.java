package com.microsoft.migration.assets.postmigration;

import com.azure.core.http.rest.PagedIterable;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import com.microsoft.migration.assets.service.AzureBlobStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;

import com.azure.storage.blob.specialized.BlobInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Post-migration integration tests for {@link AzureBlobStorageService}.
 *
 * <p>Verifies Azure Blob Storage SDK call correctness and Azure Service Bus integration
 * at the SDK boundary, using Mockito to mock the Azure SDK clients.
 *
 * <p>All fixture data is loaded from {@code testdata/shared} via Layer 2 post-migration
 * manifest entries — no inline payload bytes.
 *
 * <p>POST-MIGRATION ONLY — added in Phase 3.
 */
@ExtendWith(MockitoExtension.class)
class AzureBlobStorageServicePostMigrationIT {

    @Mock
    private BlobServiceClient blobServiceClient;
    @Mock
    private BlobContainerClient containerClient;
    @Mock
    private BlobClient blobClient;
    @Mock
    private ServiceBusTemplate serviceBusTemplate;
    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    private AzureBlobStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new AzureBlobStorageService(blobServiceClient, serviceBusTemplate, imageMetadataRepository);
        Field containerNameField = AzureBlobStorageService.class.getDeclaredField("containerName");
        containerNameField.setAccessible(true);
        containerNameField.set(service, "test-container");

        lenient().when(blobServiceClient.getBlobContainerClient("test-container")).thenReturn(containerClient);
        lenient().when(containerClient.getBlobClient(any())).thenReturn(blobClient);
    }

    // ----- storage type -----

    @Test
    void storageType_isAzureBlob() throws Exception {
        assertThat(service.getStorageType())
                .isEqualTo(PostMigrationFixtures.getExpectedStorageType("azure-upload-small-jpg"));
    }

    // ----- upload: blob SDK calls -----

    @Test
    void uploadObject_uploadsWithCorrectContentType() throws Exception {
        byte[] bytes = PostMigrationFixtures.loadPayloadBytes("azure-upload-small-jpg");
        String expectedContentType = PostMigrationFixtures.getExpectedContentType("azure-upload-small-jpg");
        MockMultipartFile file = new MockMultipartFile("file", "sample-small.jpg", expectedContentType, bytes);

        service.uploadObject(file);

        ArgumentCaptor<BlobParallelUploadOptions> optionsCaptor =
                ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(optionsCaptor.capture(), any(), any());
        BlobHttpHeaders headers = optionsCaptor.getValue().getHeaders();
        assertThat(headers).isNotNull();
        assertThat(headers.getContentType()).isEqualTo(expectedContentType);
    }

    @Test
    void uploadObject_uploadsWithCorrectContentType_png() throws Exception {
        byte[] bytes = PostMigrationFixtures.loadPayloadBytes("azure-upload-medium-png");
        String expectedContentType = PostMigrationFixtures.getExpectedContentType("azure-upload-medium-png");
        MockMultipartFile file = new MockMultipartFile("file", "sample-medium.png", expectedContentType, bytes);

        service.uploadObject(file);

        ArgumentCaptor<BlobParallelUploadOptions> optionsCaptor =
                ArgumentCaptor.forClass(BlobParallelUploadOptions.class);
        verify(blobClient).uploadWithResponse(optionsCaptor.capture(), any(), any());
        assertThat(optionsCaptor.getValue().getHeaders().getContentType()).isEqualTo(expectedContentType);
    }

    // ----- upload: Service Bus message -----

    @Test
    @SuppressWarnings("unchecked")
    void uploadObject_sendsToServiceBusWithCorrectPayload() throws Exception {
        byte[] bytes = PostMigrationFixtures.loadPayloadBytes("azure-upload-small-jpg");
        String expectedContentType = PostMigrationFixtures.getExpectedContentType("azure-upload-small-jpg");
        MockMultipartFile file = new MockMultipartFile("file", "sample-small.jpg", expectedContentType, bytes);

        service.uploadObject(file);

        ArgumentCaptor<Message<ImageProcessingMessage>> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(serviceBusTemplate).send(eq(QUEUE_NAME), msgCaptor.capture());
        ImageProcessingMessage payload = msgCaptor.getValue().getPayload();
        assertThat(payload.getContentType()).isEqualTo(expectedContentType);
        assertThat(payload.getStorageType()).isEqualTo(PostMigrationFixtures.getExpectedStorageType("azure-upload-small-jpg"));
        assertThat(payload.getSize()).isEqualTo(bytes.length);
        assertThat(payload.getKey()).isNotBlank();
    }

    // ----- upload: URL format -----

    @Test
    void uploadObject_generatesCorrectUrlFormat() throws Exception {
        byte[] bytes = PostMigrationFixtures.loadPayloadBytes("azure-upload-small-jpg");
        MockMultipartFile file = new MockMultipartFile("file", "sample-small.jpg", "image/jpeg", bytes);
        ArgumentCaptor<ImageMetadata> metaCaptor = ArgumentCaptor.forClass(ImageMetadata.class);

        service.uploadObject(file);

        verify(imageMetadataRepository).save(metaCaptor.capture());
        String urlPrefix = PostMigrationFixtures.getExpectedUrlPrefix("azure-upload-small-jpg");
        assertThat(metaCaptor.getValue().getS3Url()).startsWith(urlPrefix);
    }

    // ----- getObject -----

    @Test
    void getObject_delegatesToBlobOpenInputStream() throws Exception {
        BlobInputStream mockStream = mock(BlobInputStream.class);
        when(blobClient.openInputStream()).thenReturn(mockStream);

        InputStream result = service.getObject("test-key.jpg");

        assertThat(result).isSameAs(mockStream);
        verify(blobClient).openInputStream();
    }

    // ----- deleteObject -----

    @Test
    void deleteObject_deletesBothOriginalAndThumbnailBlob() throws Exception {
        when(blobClient.deleteIfExists()).thenReturn(true);
        when(imageMetadataRepository.findByS3Key(any())).thenReturn(Optional.empty());

        service.deleteObject("test-key.jpg");

        // Verify getBlobClient called for original and thumbnail key
        // The thumbnail key for "test-key.jpg" is "test-key_thumbnail.jpg"
        verify(containerClient).getBlobClient("test-key.jpg");
        verify(containerClient).getBlobClient("test-key_thumbnail.jpg");
        verify(blobClient, atLeastOnce()).deleteIfExists();
    }

    @Test
    void deleteObject_deletesMetadataFromRepository() throws Exception {
        when(blobClient.deleteIfExists()).thenReturn(true);
        ImageMetadata metadata = new ImageMetadata();
        metadata.setId("meta-id");
        metadata.setS3Key("test-key.jpg");
        when(imageMetadataRepository.findByS3Key("test-key.jpg")).thenReturn(Optional.of(metadata));

        service.deleteObject("test-key.jpg");

        verify(imageMetadataRepository).delete(metadata);
    }

    // ----- listObjects -----

    @Test
    @SuppressWarnings("unchecked")
    void listObjects_emptyContainer_returnsEmptyList() throws Exception {
        PagedIterable<BlobItem> emptyIterable = mock(PagedIterable.class);
        when(emptyIterable.spliterator()).thenReturn(Collections.<BlobItem>emptyList().spliterator());
        when(containerClient.listBlobs()).thenReturn(emptyIterable);
        when(imageMetadataRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(service.listObjects()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listObjects_singleBlob_returnsCorrectItem() throws Exception {
        BlobItem blobItem = mock(BlobItem.class);
        BlobItemProperties blobProperties = mock(BlobItemProperties.class);
        when(blobItem.getName()).thenReturn("uploads/test-key.jpg");
        when(blobItem.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getLastModified()).thenReturn(OffsetDateTime.now());
        when(blobProperties.getContentLength()).thenReturn(1024L);

        PagedIterable<BlobItem> iterable = mock(PagedIterable.class);
        when(iterable.spliterator()).thenReturn(Collections.singletonList(blobItem).spliterator());
        when(containerClient.listBlobs()).thenReturn(iterable);
        when(imageMetadataRepository.findAll()).thenReturn(Collections.emptyList());

        var items = service.listObjects();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getKey()).isEqualTo("uploads/test-key.jpg");
        assertThat(items.get(0).getSize()).isEqualTo(1024L);
        String urlPrefix = PostMigrationFixtures.getExpectedUrlPrefix("azure-upload-small-jpg");
        assertThat(items.get(0).getUrl()).startsWith(urlPrefix);
    }
}
