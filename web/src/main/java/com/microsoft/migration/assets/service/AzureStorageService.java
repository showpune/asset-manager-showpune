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
import com.microsoft.migration.assets.model.StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

@Service
@RequiredArgsConstructor
@Profile("!dev") // Active when not in dev profile
public class AzureStorageService implements StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureStorageService.class);
    
    private final BlobServiceClient blobServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Value("${azure.storage.container.name}")
    private String containerName;
    
    private BlobContainerClient getContainerClient() {
        return blobServiceClient.getBlobContainerClient(containerName);
    }
    
    @Override
    public List<StorageItem> listObjects() {
        try {
            BlobContainerClient containerClient = getContainerClient();
            
            return containerClient.listBlobs().stream()
                    .map(blobItem -> {
                        // Try to get metadata for upload time
                        var uploadedAt = imageMetadataRepository.findAll().stream()
                                .filter(metadata -> metadata.getStorageKey() != null && metadata.getStorageKey().equals(blobItem.getName()))
                                .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                                .findFirst()
                                .orElse(blobItem.getProperties().getLastModified().toInstant());

                        return new StorageItem(
                                blobItem.getName(),
                                extractFilename(blobItem.getName()),
                                blobItem.getProperties().getContentLength(),
                                blobItem.getProperties().getLastModified().toInstant(),
                                uploadedAt,
                                generateUrl(blobItem.getName())
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list blobs", e);
            return List.of();
        }
    }
    
    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());
        
        BlobClient blobClient = getContainerClient().getBlobClient(key);
        
        // Upload the file
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        
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
        metadata.setStorageKey(key);
        metadata.setStorageUrl(generateUrl(key));
        
        imageMetadataRepository.save(metadata);
    }
    
    @Override
    public InputStream getObject(String key) throws IOException {
        try {
            BlobClient blobClient = getContainerClient().getBlobClient(key);
            return blobClient.openInputStream();
        } catch (Exception e) {
            throw new IOException("Failed to get object: " + key, e);
        }
    }
    
    @Override
    public void deleteObject(String key) throws IOException {
        try {
            BlobClient blobClient = getContainerClient().getBlobClient(key);
            blobClient.deleteIfExists();
        } catch (Exception e) {
            throw new IOException("Failed to delete object: " + key, e);
        }
    }
    
    @Override
    public String getStorageType() {
        return "azure";
    }
    
    private String generateKey(String filename) {
        String uuid = UUID.randomUUID().toString();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        return uuid + extension;
    }
    
    private String generateUrl(String key) {
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
    
    private String extractFilename(String key) {
        // Extract filename from key (remove UUID prefix if present)
        if (key.contains("/")) {
            return key.substring(key.lastIndexOf("/") + 1);
        }
        return key;
    }
}