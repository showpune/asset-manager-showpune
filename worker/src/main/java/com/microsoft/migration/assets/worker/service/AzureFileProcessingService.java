package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class AzureFileProcessingService extends AbstractFileProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureFileProcessingService.class);
    
    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.container.name}")
    private String containerName;
    
    private BlobContainerClient getContainerClient() {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobClient blobClient = getContainerClient().getBlobClient(key);
        
        try (InputStream inputStream = blobClient.openInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobClient blobClient = getContainerClient().getBlobClient(key);
        
        // Upload the thumbnail
        blobClient.uploadFromFile(source.toString(), true);
        
        // Save or update thumbnail metadata
        ImageMetadata metadata = imageMetadataRepository.findById(extractOriginalKey(key))
            .orElseGet(() -> {
                ImageMetadata newMetadata = new ImageMetadata();
                newMetadata.setId(extractOriginalKey(key));
                return newMetadata;
            });

        metadata.setThumbnailKey(key);
        metadata.setThumbnailUrl(generateUrl(key));
        imageMetadataRepository.save(metadata);
    }

    @Override
    public String getStorageType() {
        return "azure";
    }

    @Override
    protected String generateUrl(String key) {
        try {
            BlobClient blobClient = getContainerClient().getBlobClient(key);
            
            // Generate SAS token for blob access
            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(1), // 1 hour expiry
                new BlobSasPermission().setReadPermission(true)
            );
            
            String sasToken = blobClient.generateSas(sasValues);
            return blobClient.getBlobUrl() + "?" + sasToken;
        } catch (Exception e) {
            logger.error("Failed to generate URL for key: " + key, e);
            return ""; // Return empty string if URL generation fails
        }
    }

    private String extractOriginalKey(String key) {
        // Remove _thumbnail suffix if present
        String suffix = "_thumbnail";
        int suffixIndex = key.lastIndexOf(suffix);
        if (suffixIndex > 0) {
            return key.substring(0, suffixIndex);
        }
        return key;
    }
}