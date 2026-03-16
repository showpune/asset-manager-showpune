package com.microsoft.migration.assets.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

@Service
@RequiredArgsConstructor
@Profile("!dev")
public class AzureBlobService implements StorageService {

    private final BlobServiceClient blobServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    private BlobContainerClient getContainerClient() {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public List<S3StorageItem> listObjects() {
        BlobContainerClient containerClient = getContainerClient();

        // Load all metadata once and index by blob key to avoid N+1 queries
        Map<String, ImageMetadata> metadataByKey = imageMetadataRepository.findAll().stream()
                .collect(Collectors.toMap(ImageMetadata::getS3Key, m -> m, (a, b) -> a));

        return StreamSupport.stream(containerClient.listBlobs().spliterator(), false)
                .map(blobItem -> {
                    ImageMetadata metadata = metadataByKey.get(blobItem.getName());
                    Instant uploadedAt = (metadata != null)
                            ? metadata.getUploadedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                            : blobItem.getProperties().getLastModified().toInstant();

                    return new S3StorageItem(
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
        String key = generateKey(file.getOriginalFilename());
        BlobClient blobClient = getContainerClient().getBlobClient(key);

        blobClient.upload(file.getInputStream(), file.getSize(), true);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(file.getContentType()));

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
        BlobClient blobClient = getContainerClient().getBlobClient(key);
        return blobClient.openInputStream();
    }

    @Override
    public void deleteObject(String key) throws IOException {
        getContainerClient().getBlobClient(key).deleteIfExists();

        try {
            getContainerClient().getBlobClient(getThumbnailKey(key)).deleteIfExists();
        } catch (Exception e) {
            // Ignore if thumbnail doesn't exist
        }

        imageMetadataRepository.findByS3Key(key)
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
        return getContainerClient().getBlobClient(key).getBlobUrl();
    }

    private String generateKey(String filename) {
        return UUID.randomUUID().toString() + "-" + filename;
    }
}
