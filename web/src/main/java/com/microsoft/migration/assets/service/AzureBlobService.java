package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

@Service
@RequiredArgsConstructor
@Profile("!dev")
public class AzureBlobService implements StorageService {

    private final BlobServiceClient blobServiceClient;
    private final JmsTemplate jmsTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.blob.container}")
    private String containerName;

    @Override
    public List<S3StorageItem> listObjects() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        return StreamSupport.stream(containerClient.listBlobs().spliterator(), false)
                .map(blobItem -> {
                    Instant uploadedAt = imageMetadataRepository.findAll().stream()
                            .filter(metadata -> metadata.getS3Key().equals(blobItem.getName()))
                            .map(metadata -> metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                            .findFirst()
                            .orElse(blobItem.getProperties().getLastModified() != null
                                    ? blobItem.getProperties().getLastModified().toInstant()
                                    : Instant.now());

                    Instant lastModified = blobItem.getProperties().getLastModified() != null
                            ? blobItem.getProperties().getLastModified().toInstant()
                            : Instant.now();

                    return new S3StorageItem(
                            blobItem.getName(),
                            extractFilename(blobItem.getName()),
                            blobItem.getProperties().getContentLength() != null
                                    ? blobItem.getProperties().getContentLength() : 0L,
                            lastModified,
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
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        ImageProcessingMessage message = new ImageProcessingMessage(
                key,
                file.getContentType(),
                getStorageType(),
                file.getSize()
        );
        jmsTemplate.convertAndSend(QUEUE_NAME, message);

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
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        return blobClient.openInputStream();
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

        imageMetadataRepository.findAll().stream()
                .filter(metadata -> metadata.getS3Key().equals(key))
                .findFirst()
                .ifPresent(imageMetadataRepository::delete);
    }

    @Override
    public String getStorageType() {
        return "blob";
    }

    private String extractFilename(String key) {
        int lastSlashIndex = key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? key.substring(lastSlashIndex + 1) : key;
    }

    private String generateUrl(String key) {
        return blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(key)
                .getBlobUrl();
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}
