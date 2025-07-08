package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Profile("azure")
@RequiredArgsConstructor
public class AzureFileProcessingService extends AbstractAzureFileProcessingService {
    
    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.container-name}")
    private String containerName;
    
    @Value("${azure.storage.endpoint}")
    private String endpoint;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        try (var inputStream = blobClient.openInputStream()) {
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

    @Override
    protected String generateUrl(String key) {
        return endpoint + "/" + containerName + "/" + key;
    }

    private String extractOriginalKey(String key) {
        // Remove the "_thumbnail" suffix to get the original key
        return key.replaceAll("_thumbnail", "");
    }
}