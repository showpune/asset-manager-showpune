package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Profile("azure")
public class AzureBlobStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(AzureBlobStorageService.class);

    private final BlobServiceClient blobServiceClient;
    private final JmsTemplate jmsTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Value("${azure.servicebus.queue-name}")
    private String queueName;

    @Override
    public List<S3StorageItem> listObjects() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        return StreamSupport.stream(containerClient.listBlobs().spliterator(), false)
                .map(blobItem -> {
                    BlobItemProperties properties = blobItem.getProperties();
                    String blobName = blobItem.getName();
                    
                    // Try to get metadata for upload time
                    Instant uploadedAt = imageMetadataRepository.findByS3Key(blobName)
                            .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                            .orElse(properties.getLastModified().toInstant());

                    return new S3StorageItem(
                            blobName,
                            extractFilename(blobName),
                            properties.getContentLength(),
                            properties.getLastModified().toInstant(),
                            uploadedAt,
                            generateUrl(blobName)
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());
        
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        
        if (file.getContentType() != null) {
            blobClient.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                    .setContentType(file.getContentType()));
        }

        // Send message to queue for thumbnail generation
        ImageProcessingMessage message = new ImageProcessingMessage(
                key,
                file.getContentType(),
                getStorageType(),
                file.getSize()
        );
        jmsTemplate.convertAndSend(queueName, message);
        logger.info("Sent message to Azure Service Bus queue: {}", queueName);

        // Create and save metadata to database
        ImageMetadata metadata = new ImageMetadata();
        metadata.setId(UUID.randomUUID().toString());
        metadata.setFilename(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setS3Key(key);
        metadata.setS3Url(generateUrl(key));
        
        imageMetadataRepository.save(metadata);
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(key);
            return blobClient.openInputStream();
        } catch (Exception e) {
            throw new IOException("Failed to get object from Azure Blob Storage: " + key, e);
        }
    }

    @Override
    public void deleteObject(String key) throws IOException {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(key);
            blobClient.delete();

            // Try to delete thumbnail if it exists
            try {
                BlobClient thumbnailClient = containerClient.getBlobClient(getThumbnailKey(key));
                if (thumbnailClient.exists()) {
                    thumbnailClient.delete();
                }
            } catch (Exception e) {
                logger.warn("Could not delete thumbnail for {}: {}", key, e.getMessage());
            }

            // Delete metadata from database
            imageMetadataRepository.findByS3Key(key)
                    .ifPresent(metadata -> imageMetadataRepository.delete(metadata));
            
        } catch (Exception e) {
            throw new IOException("Failed to delete object from Azure Blob Storage: " + key, e);
        }
    }

    @Override
    public String getStorageType() {
        return "azure-blob";
    }

    private String extractFilename(String key) {
        int lastSlashIndex = key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? key.substring(lastSlashIndex + 1) : key;
    }

    private String generateUrl(String key) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        return blobClient.getBlobUrl();
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}
