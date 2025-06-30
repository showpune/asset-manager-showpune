package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Profile("azure") // Active when azure profile is active
public class AzureBlobService implements StorageService {

    private final BlobServiceClient blobServiceClient;
    private final ServiceBusTemplate serviceBusTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.container.name}")
    private String containerName;

    @Value("${azure.servicebus.queue.name}")
    private String queueName;

    @Override
    public List<S3StorageItem> listObjects() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        return containerClient.listBlobs().stream()
                .map(this::mapBlobToS3StorageItem)
                .collect(Collectors.toList());
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());
        
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);

        // Upload file to Azure Blob Storage
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Send message to Azure Service Bus for thumbnail generation
        ImageProcessingMessage message = new ImageProcessingMessage(
            key,
            file.getContentType(),
            getStorageType(),
            file.getSize()
        );
        
        Message<ImageProcessingMessage> serviceBusMessage = MessageBuilder
                .withPayload(message)
                .build();
        serviceBusTemplate.send(queueName, serviceBusMessage);

        // Create and save metadata to database
        ImageMetadata metadata = new ImageMetadata();
        metadata.setId(UUID.randomUUID().toString());
        metadata.setFilename(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setS3Key(key); // Keep same field name for compatibility
        metadata.setS3Url(generateUrl(key)); // Keep same field name for compatibility
        
        imageMetadataRepository.save(metadata);
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        return blobClient.downloadContent().toStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        // Delete original blob
        BlobClient blobClient = containerClient.getBlobClient(key);
        blobClient.deleteIfExists();

        // Try to delete thumbnail if it exists
        try {
            BlobClient thumbnailBlobClient = containerClient.getBlobClient(getThumbnailKey(key));
            thumbnailBlobClient.deleteIfExists();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        // Delete metadata from database
        imageMetadataRepository.findAll().stream()
                .filter(metadata -> metadata.getS3Key().equals(key))
                .findFirst()
                .ifPresent(metadata -> imageMetadataRepository.delete(metadata));
    }

    @Override
    public String getStorageType() {
        return "azure";
    }

    private S3StorageItem mapBlobToS3StorageItem(BlobItem blobItem) {
        // Try to get metadata for upload time
        java.time.Instant uploadedAt = imageMetadataRepository.findAll().stream()
                .filter(metadata -> metadata.getS3Key().equals(blobItem.getName()))
                .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                .findFirst()
                .orElse(blobItem.getProperties().getLastModified().toInstant()); // Convert OffsetDateTime to Instant

        return new S3StorageItem(
                blobItem.getName(),
                extractFilename(blobItem.getName()),
                blobItem.getProperties().getContentLength(),
                blobItem.getProperties().getLastModified().toInstant(), // Convert OffsetDateTime to Instant
                uploadedAt,
                generateUrl(blobItem.getName())
        );
    }

    private String extractFilename(String key) {
        // Extract filename from the blob key
        int lastSlashIndex = key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? key.substring(lastSlashIndex + 1) : key;
    }

    private String generateUrl(String key) {
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

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}