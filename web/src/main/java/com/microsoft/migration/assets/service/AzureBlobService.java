package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private final BlobContainerClient blobContainerClient;
    private final RabbitTemplate rabbitTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Override
    public List<S3StorageItem> listObjects() {
        return blobContainerClient.listBlobs().stream()
                .map(blobItem -> {
                    BlobClient blobClient = blobContainerClient.getBlobClient(blobItem.getName());
                    BlobProperties properties = blobClient.getProperties();

                    // Try to get metadata for upload time
                    Instant uploadedAt = imageMetadataRepository.findAll().stream()
                            .filter(metadata -> metadata.getS3Key().equals(blobItem.getName()))
                            .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                            .findFirst()
                            .orElse(properties.getLastModified().toInstant()); // fallback to lastModified if metadata not found

                    return new S3StorageItem(
                            blobItem.getName(),
                            extractFilename(blobItem.getName()),
                            properties.getBlobSize(),
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

        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Set content type
        blobClient.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                .setContentType(file.getContentType()));

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
        metadata.setS3Key(key);
        metadata.setS3Url(generateUrl(key));

        imageMetadataRepository.save(metadata);
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        return blobClient.openInputStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        // Delete original blob
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        blobClient.delete();

        try {
            // Try to delete thumbnail if it exists
            BlobClient thumbnailClient = blobContainerClient.getBlobClient(getThumbnailKey(key));
            if (thumbnailClient.exists()) {
                thumbnailClient.delete();
            }
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
        return "azure-blob";
    }

    private String extractFilename(String key) {
        // Extract filename from the object key
        int lastSlashIndex = key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? key.substring(lastSlashIndex + 1) : key;
    }

    private String generateUrl(String key) {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        return blobClient.getBlobUrl();
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}
