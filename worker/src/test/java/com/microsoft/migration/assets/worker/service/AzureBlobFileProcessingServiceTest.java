package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AzureBlobFileProcessingServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;
    
    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @Test
    void shouldCreateServiceInstance() {
        // Given/When
        AzureBlobFileProcessingService service = new AzureBlobFileProcessingService(
            blobServiceClient, imageMetadataRepository
        );

        // Then
        assertNotNull(service);
        assertEquals("azure-blob", service.getStorageType());
    }
}