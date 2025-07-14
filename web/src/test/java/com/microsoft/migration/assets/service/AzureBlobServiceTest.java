package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AzureBlobServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    private AzureBlobService azureBlobService;

    @BeforeEach
    void setUp() {
        azureBlobService = new AzureBlobService(blobServiceClient, rabbitTemplate, imageMetadataRepository);
        ReflectionTestUtils.setField(azureBlobService, "containerName", "test-container");
    }

    @Test
    void shouldReturnAzureAsStorageType() {
        // When
        String storageType = azureBlobService.getStorageType();
        
        // Then
        assertEquals("azure", storageType);
    }

    @Test
    void shouldGenerateThumbnailKey() {
        // Given
        String originalKey = "image.jpg";
        
        // When
        String thumbnailKey = azureBlobService.getThumbnailKey(originalKey);
        
        // Then
        assertEquals("image_thumbnail.jpg", thumbnailKey);
    }

    @Test
    void shouldGenerateThumbnailKeyWithoutExtension() {
        // Given
        String originalKey = "image";
        
        // When
        String thumbnailKey = azureBlobService.getThumbnailKey(originalKey);
        
        // Then
        assertEquals("image_thumbnail", thumbnailKey);
    }
}