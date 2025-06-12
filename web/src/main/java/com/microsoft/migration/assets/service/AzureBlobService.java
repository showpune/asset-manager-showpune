package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.microsoft.migration.assets.model.BlobStorageItem;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

@Service
@RequiredArgsConstructor
@Profile("!dev") // Active when not in dev profile
public class AzureBlobService implements StorageService {

    private final BlobServiceClient blobServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.container}")
    private String containerName;

    @Override
    public List<BlobStorageItem> listObjects() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        return containerClient.listBlobs().stream()
                .map(blobItem -> {
                    BlobItemProperties properties = blobItem.getProperties();
                    
                    // Try to get metadata for upload time
                    Instant uploadedAt = imageMetadataRepository.findAll().stream()
                            .filter(metadata -> metadata.getBlobKey().equals(blobItem.getName()))
                            .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                            .findFirst()
                            .orElse(properties.getLastModified().toInstant()); // fallback to lastModified if metadata not found

                    return new BlobStorageItem(
                            blobItem.getName(),
                            extractFilename(blobItem.getName()),
                            properties.getContentLength(),
                            properties.getLastModified().toInstant(),
                            uploadedAt,
                            generateUrl(blobItem.getName())
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());
        
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        // Upload file with content type
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        
        // Set content type
        blobClient.getProperties(); // This will set the content type from the blob

        // Send message to queue for thumbnail generation
        ImageProcessingMessage message = new ImageProcessingMessage(
            key,
            file.getContentType(),
            getStorageType(),
            file.getSize()
        );
        rabbitTemplate.convertAndSend(QUEUE_NAME, message);

        // Create and save metadata to database
        ImageMetadata metadata = new ImageMetadata();
        metadata.setId(UUID.randomUUID().toString());
        metadata.setFilename(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setBlobKey(key);
        metadata.setBlobUrl(generateUrl(key));
        
        imageMetadataRepository.save(metadata);
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        return blobClient.openInputStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        // Delete both original and thumbnail if it exists
        BlobClient blobClient = containerClient.getBlobClient(key);
        blobClient.deleteIfExists();

        try {
            // Try to delete thumbnail if it exists
            BlobClient thumbnailBlobClient = containerClient.getBlobClient(getThumbnailKey(key));
            thumbnailBlobClient.deleteIfExists();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        // Delete metadata from database
        imageMetadataRepository.findAll().stream()
                .filter(metadata -> metadata.getBlobKey().equals(key))
                .findFirst()
                .ifPresent(metadata -> imageMetadataRepository.delete(metadata));
    }

    @Override
    public String getStorageType() {
        return "azure";
    }

    private String extractFilename(String key) {
        // Extract filename from the object key
        int lastSlashIndex = key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? key.substring(lastSlashIndex + 1) : key;
    }
    
    private String generateUrl(String key) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        // Generate a SAS URL for the blob (valid for 1 hour)
        BlobSasPermission sasPermission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(1), sasPermission);
                
        String sasToken = blobClient.generateSas(sasValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}