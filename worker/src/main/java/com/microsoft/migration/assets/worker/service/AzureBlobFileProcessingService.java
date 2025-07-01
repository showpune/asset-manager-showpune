package com.microsoft.migration.assets.worker.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Profile("azure")
@RequiredArgsConstructor
public class AzureBlobFileProcessingService extends AbstractFileProcessingService {
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.endpoint}")
    private String storageEndpoint;

    @Value("${azure.storage.container-name}")
    private String containerName;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;

    @PostConstruct
    public void init() {
        // Initialize Azure Blob Storage client using DefaultAzureCredential
        blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(storageEndpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        
        containerClient = blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public void downloadOriginal(String blobName, Path destination) throws Exception {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.downloadToFile(destination.toString(), true);
    }

    @Override
    public void uploadThumbnail(Path source, String blobName, String contentType) throws Exception {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        
        // Upload thumbnail to Azure Blob Storage
        BlobParallelUploadOptions uploadOptions = new BlobParallelUploadOptions(Files.newInputStream(source));
        uploadOptions.setHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                .setContentType(contentType));
        
        blobClient.uploadWithResponse(uploadOptions, null, null);
        
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
        return "azure";
    }

    @Override
    protected String generateUrl(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
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