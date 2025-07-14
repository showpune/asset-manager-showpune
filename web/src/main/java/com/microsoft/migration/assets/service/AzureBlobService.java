package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.BlobStorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
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
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        
        ListBlobsOptions options = new ListBlobsOptions().setPrefix("");
        
        return blobContainerClient.listBlobs(options, null).stream()
                .map(blobItem -> {
                    // Try to get metadata for upload time
                    Instant uploadedAt = imageMetadataRepository.findAll().stream()
                            .filter(metadata -> metadata.getS3Key().equals(blobItem.getName()))
                            .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                            .findFirst()
                            .orElse(blobItem.getProperties().getLastModified().toInstant()); // fallback to lastModified if metadata not found

                    return new BlobStorageItem(
                            blobItem.getName(),
                            extractFilename(blobItem.getName()),
                            blobItem.getProperties().getContentLength(),
                            blobItem.getProperties().getLastModified().toInstant(),
                            uploadedAt,
                            generateUrl(blobItem.getName())
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String blobName = generateBlobName(file.getOriginalFilename());
        
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
        
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Send message to queue for thumbnail generation
        ImageProcessingMessage message = new ImageProcessingMessage(
            blobName,
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
        metadata.setS3Key(blobName);
        metadata.setS3Url(generateUrl(blobName));
        
        imageMetadataRepository.save(metadata);
    }

    @Override
    public InputStream getObject(String blobName) throws IOException {
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
        
        return blobClient.openInputStream();
    }

    @Override
    public void deleteObject(String blobName) throws IOException {
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
        
        blobClient.deleteIfExists();

        try {
            // Try to delete thumbnail if it exists
            var thumbnailBlobClient = blobContainerClient.getBlobClient(getThumbnailKey(blobName));
            thumbnailBlobClient.deleteIfExists();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        // Delete metadata from database
        imageMetadataRepository.findAll().stream()
                .filter(metadata -> metadata.getS3Key().equals(blobName))
                .findFirst()
                .ifPresent(metadata -> imageMetadataRepository.delete(metadata));
    }

    @Override
    public String getStorageType() {
        return "blob";
    }

    private String extractFilename(String blobName) {
        // Extract filename from the blob name
        int lastSlashIndex = blobName.lastIndexOf('/');
        return lastSlashIndex >= 0 ? blobName.substring(lastSlashIndex + 1) : blobName;
    }
    
    private String generateUrl(String blobName) {
        var blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        var blobClient = blobContainerClient.getBlobClient(blobName);
        return blobClient.getBlobUrl();
    }

    private String generateBlobName(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}