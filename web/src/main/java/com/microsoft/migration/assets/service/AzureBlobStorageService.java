package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.model.ImageMetadata;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
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
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

@Service
@RequiredArgsConstructor
@Profile("!dev")
public class AzureBlobStorageService implements StorageService {

    private final BlobServiceClient blobServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Override
    public List<S3StorageItem> listObjects() {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        return StreamSupport.stream(containerClient.listBlobs().spliterator(), false)
                .map(blobItem -> {
                    String key = blobItem.getName();
                    long size = blobItem.getProperties().getContentLength() != null
                            ? blobItem.getProperties().getContentLength() : 0L;
                    Instant lastModified = blobItem.getProperties().getLastModified() != null
                            ? blobItem.getProperties().getLastModified().toInstant()
                            : Instant.now();

                    Instant uploadedAt = imageMetadataRepository.findByS3Key(key)
                            .map(metadata -> metadata.getUploadedAt()
                                    .atZone(ZoneId.systemDefault()).toInstant())
                            .orElse(lastModified);

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
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key);
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        ImageProcessingMessage message = new ImageProcessingMessage(
                key,
                file.getContentType(),
                getStorageType(),
                file.getSize()
        );
        rabbitTemplate.convertAndSend(QUEUE_NAME, message);

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
        return blobClient.openInputStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key);
        blobClient.delete();

        try {
            BlobClient thumbnailClient = blobServiceClient.getBlobContainerClient(containerName)
                    .getBlobClient(getThumbnailKey(key));
            thumbnailClient.delete();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        imageMetadataRepository.findByS3Key(key)
                .ifPresent(imageMetadataRepository::delete);
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
        return blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key).getBlobUrl();
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}
