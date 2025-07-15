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
    void getStorageType_ReturnsAzure() {
        // Given
        AzureBlobStorageService service = new AzureBlobStorageService(
            blobServiceClient, rabbitTemplate, imageMetadataRepository);
        
        // When
        String storageType = service.getStorageType();
        
        // Then
        assertEquals("azure", storageType);
    }

    @Test
    void getThumbnailKey_AddsCorrectSuffix() {
        // Given
        AzureBlobStorageService service = new AzureBlobStorageService(
            blobServiceClient, rabbitTemplate, imageMetadataRepository);
        String originalKey = "test-image.jpg";
        
        // When
        String thumbnailKey = service.getThumbnailKey(originalKey);
        
        // Then
        assertEquals("test-image_thumbnail.jpg", thumbnailKey);
    }

    @Test
    void getThumbnailKey_HandlesKeyWithoutExtension() {
        // Given
        AzureBlobStorageService service = new AzureBlobStorageService(
            blobServiceClient, rabbitTemplate, imageMetadataRepository);
        String originalKey = "test-image";
        
        // When
        String thumbnailKey = service.getThumbnailKey(originalKey);
        
        // Then
        assertEquals("test-image_thumbnail", thumbnailKey);
    }
}