package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AzureBlobFileProcessingServiceTest {

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    private AzureBlobFileProcessingService azureBlobFileProcessingService;

    @BeforeEach
    void setUp() {
        azureBlobFileProcessingService = new AzureBlobFileProcessingService(blobServiceClient, imageMetadataRepository);
        ReflectionTestUtils.setField(azureBlobFileProcessingService, "containerName", "test-container");
    }

    @Test
    void shouldReturnAzureAsStorageType() {
        // When
        String storageType = azureBlobFileProcessingService.getStorageType();
        
        // Then
        assertEquals("azure", storageType);
    }
}