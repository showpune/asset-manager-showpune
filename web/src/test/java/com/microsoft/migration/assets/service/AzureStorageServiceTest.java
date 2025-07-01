package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("azure")
class AzureStorageServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @InjectMocks
    private AzureStorageService azureStorageService;

    @Test
    void testGetStorageType() {
        // Test that the service returns the correct storage type
        String storageType = azureStorageService.getStorageType();
        assertEquals("azure", storageType);
    }

    @Test
    void testGetThumbnailKeyWithExtension() {
        // Test thumbnail key generation for files with extension
        String originalKey = "image.jpg";
        String thumbnailKey = azureStorageService.getThumbnailKey(originalKey);
        assertEquals("image_thumbnail.jpg", thumbnailKey);
    }

    @Test
    void testGetThumbnailKeyWithoutExtension() {
        // Test thumbnail key generation for files without extension
        String originalKey = "image";
        String thumbnailKey = azureStorageService.getThumbnailKey(originalKey);
        assertEquals("image_thumbnail", thumbnailKey);
    }
}