package com.microsoft.migration.assets.worker.service;

import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class AzureBlobFileProcessingService extends AbstractFileProcessingService {
    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.container}")
    private String containerName;

    @Override
    public void downloadOriginal(String blobName, Path destination) throws Exception {
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
                
        try (var inputStream = blobClient.openInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String blobName, String contentType) throws Exception {
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
                
        blobClient.uploadFromFile(source.toString(), true);
        
        // Save or update thumbnail metadata
        ImageMetadata metadata = imageMetadataRepository.findById(extractOriginalKey(blobName))
            .orElseGet(() -> {
                ImageMetadata newMetadata = new ImageMetadata();
                newMetadata.setId(extractOriginalKey(blobName));
                return newMetadata;
            });

        metadata.setThumbnailKey(blobName);
        metadata.setThumbnailUrl(generateUrl(blobName));
        imageMetadataRepository.save(metadata);
    }

    @Override
    public String getStorageType() {
        return "blob";
    }

    @Override
    protected String generateUrl(String blobName) {
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
        return blobClient.getBlobUrl();
    }

    private String extractOriginalKey(String blobName) {
        // Remove _thumbnail suffix if present
        String suffix = "_thumbnail";
        int suffixIndex = blobName.lastIndexOf(suffix);
        if (suffixIndex > 0) {
            return blobName.substring(0, suffixIndex);
        }
        return blobName;
    }
}