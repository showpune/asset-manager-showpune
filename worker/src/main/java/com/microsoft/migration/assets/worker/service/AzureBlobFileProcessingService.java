package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;

@Service
@Profile("azure")
@RequiredArgsConstructor
@Slf4j
public class AzureBlobFileProcessingService extends AbstractFileProcessingService {
    
    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.container.name}")
    private String containerName;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
                
        try (var inputStream = blobClient.downloadContent().toStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
                
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

    public void processImageMessage(com.microsoft.migration.assets.worker.model.ImageProcessingMessage message) {
        // Call the existing image processing logic from the abstract class
        // We need to expose this functionality somehow
        java.nio.file.Path tempDir = null;
        java.nio.file.Path originalFile = null;
        java.nio.file.Path thumbnailFile = null;

        try {
            tempDir = java.nio.file.Files.createTempDirectory("image-processing");
            originalFile = tempDir.resolve("original" + com.microsoft.migration.assets.worker.util.StorageUtil.getExtension(message.getKey()));
            thumbnailFile = tempDir.resolve("thumbnail" + com.microsoft.migration.assets.worker.util.StorageUtil.getExtension(message.getKey()));

            // Download original file
            downloadOriginal(message.getKey(), originalFile);

            // Generate thumbnail
            generateThumbnail(originalFile, thumbnailFile);

            // Upload thumbnail
            String thumbnailKey = com.microsoft.migration.assets.worker.util.StorageUtil.getThumbnailKey(message.getKey());
            uploadThumbnail(thumbnailFile, thumbnailKey, message.getContentType());

        } catch (Exception e) {
            throw new RuntimeException("Failed to process image: " + message.getKey(), e);
        } finally {
            try {
                // Cleanup temporary files
                if (originalFile != null) {
                    java.nio.file.Files.deleteIfExists(originalFile);
                }
                if (thumbnailFile != null) {
                    java.nio.file.Files.deleteIfExists(thumbnailFile);
                }
                if (tempDir != null) {
                    java.nio.file.Files.deleteIfExists(tempDir);
                }
            } catch (java.io.IOException e) {
                log.error("Error cleaning up temporary files for: {}", message.getKey(), e);
            }
        }
    }

    @Override
    protected String generateUrl(String key) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        // Generate SAS URL with read permissions valid for 1 hour
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(1),
                new BlobSasPermission().setReadPermission(true)
        );
        
        String sasToken = blobClient.generateSas(sasValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
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