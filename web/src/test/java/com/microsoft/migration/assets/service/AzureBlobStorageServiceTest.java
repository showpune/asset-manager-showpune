package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AzureBlobStorageServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;
    
    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @Test
    void shouldCreateServiceInstance() {
        // Given/When
        AzureBlobStorageService service = new AzureBlobStorageService(
            blobServiceClient, rabbitTemplate, imageMetadataRepository
        );

        // Then
        assertNotNull(service);
        assertEquals("azure-blob", service.getStorageType());
    }

    @Test
    void shouldGenerateThumbnailKey() {
        // Given
        AzureBlobStorageService service = new AzureBlobStorageService(
            blobServiceClient, rabbitTemplate, imageMetadataRepository
        );
        
        // When
        String thumbnailKey = service.getThumbnailKey("test-image.jpg");
        
        // Then
        assertTrue(thumbnailKey.contains("_thumbnail"));
        assertTrue(thumbnailKey.endsWith(".jpg"));
    }
}