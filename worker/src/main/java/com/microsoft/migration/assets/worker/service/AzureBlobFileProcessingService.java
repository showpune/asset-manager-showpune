package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class AzureBlobFileProcessingService extends AbstractFileProcessingService {
    
    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.container.name}")
    private String containerName;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        blobClient.downloadToFile(destination.toString(), true);
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        String thumbnailKey = getThumbnailKey(key);
        
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient thumbnailClient = containerClient.getBlobClient(thumbnailKey);
        
        thumbnailClient.uploadFromFile(source.toString(), true);

        // Update metadata in database with thumbnail info
        String originalKey = extractOriginalKey(key);
        ImageMetadata metadata = imageMetadataRepository.findAll().stream()
                .filter(m -> m.getStorageKey().equals(originalKey))
                .findFirst()
                .orElse(null);

        if (metadata != null) {
            metadata.setThumbnailKey(thumbnailKey);
            metadata.setThumbnailUrl(generateUrl(thumbnailKey));
            imageMetadataRepository.save(metadata);
        }
    }

    @Override
    public String getStorageType() {
        return "azure";
    }

    @Override
    protected String generateUrl(String key) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        // Generate a SAS URL with read permissions valid for 1 hour
        BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(1), sasPermission);
        
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

    private String getThumbnailKey(String originalKey) {
        int dotIndex = originalKey.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalKey.substring(0, dotIndex) + "_thumbnail" + originalKey.substring(dotIndex);
        }
        return originalKey + "_thumbnail";
    }
}