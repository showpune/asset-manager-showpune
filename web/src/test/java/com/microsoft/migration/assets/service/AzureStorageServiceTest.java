package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.model.StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("!dev") // Ensure we are not in dev profile
class AzureStorageServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;
    
    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @Test
    void testServiceCreation() {
        // Test that the service can be created successfully
        AzureStorageService service = new AzureStorageService(
            blobServiceClient, 
            rabbitTemplate, 
            imageMetadataRepository
        );
        
        assertNotNull(service);
        assertEquals("azure", service.getStorageType());
    }

    @Test
    void testGetThumbnailKey() {
        AzureStorageService service = new AzureStorageService(
            blobServiceClient, 
            rabbitTemplate, 
            imageMetadataRepository
        );
        
        String key = "test-image.jpg";
        String thumbnailKey = service.getThumbnailKey(key);
        assertEquals("test-image_thumbnail.jpg", thumbnailKey);
    }
}
