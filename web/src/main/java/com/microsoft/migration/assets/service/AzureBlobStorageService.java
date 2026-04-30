package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

@Service
@RequiredArgsConstructor
@Profile("!dev & !baseline")
public class AzureBlobStorageService implements StorageService {

    private final BlobServiceClient blobServiceClient;
    private final ServiceBusTemplate serviceBusTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.blob.container.name}")
    private String containerName;

    @Override
    public List<S3StorageItem> listObjects() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // Load all metadata once to avoid N+1 queries
        Map<String, Instant> keyToUploadedAt = imageMetadataRepository.findAll().stream()
                .filter(m -> m.getS3Key() != null)
                .collect(Collectors.toMap(
                        ImageMetadata::getS3Key,
                        m -> m.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                        (existing, replacement) -> existing));

        return StreamSupport.stream(containerClient.listBlobs().spliterator(), false)
                .map(blobItem -> {
                    String key = blobItem.getName();
                    Instant lastModified = blobItem.getProperties().getLastModified() != null
                            ? blobItem.getProperties().getLastModified().toInstant()
                            : Instant.now();

                    Instant uploadedAt = keyToUploadedAt.getOrDefault(key, lastModified);

                    long size = blobItem.getProperties().getContentLength() != null
                            ? blobItem.getProperties().getContentLength()
                            : 0L;

                    return new S3StorageItem(
                            key,
                            extractFilename(key),
                            size,
                            lastModified,
                            uploadedAt,
                            generateUrl(key)
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);

        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);
        }

        // Send message to queue for thumbnail generation
        ImageProcessingMessage message = new ImageProcessingMessage(
                key,
                file.getContentType(),
                getStorageType(),
                file.getSize()
        );
        Message<ImageProcessingMessage> msg = MessageBuilder.withPayload(message).build();
        serviceBusTemplate.send(QUEUE_NAME, msg);

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
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key);
        return blobClient.downloadContent().toStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        containerClient.getBlobClient(key).deleteIfExists();

        try {
            containerClient.getBlobClient(getThumbnailKey(key)).deleteIfExists();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        // Delete metadata from database
        imageMetadataRepository.findByS3Key(key)
                .ifPresent(metadata -> imageMetadataRepository.delete(metadata));
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
        return "/s3/view/" + key;
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}
