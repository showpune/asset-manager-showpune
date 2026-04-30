package com.microsoft.migration.assets.worker.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import com.microsoft.migration.assets.worker.util.StorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

@Service
@Profile("!dev & !baseline")
@RequiredArgsConstructor
public class AzureBlobStorageFileProcessingService extends AbstractFileProcessingService {

    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.blob.container.name}")
    private String containerName;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key);
        blobClient.downloadToFile(destination.toString(), true);
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);

        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
        BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromFile(source))
                .setHeaders(headers);
        blobClient.uploadWithResponse(options, null, null);

        // Save or update thumbnail metadata, linking to the original upload record
        String originalKey = extractOriginalKey(key);
        ImageMetadata metadata = imageMetadataRepository.findByS3Key(originalKey)
                .orElseGet(() -> {
                    ImageMetadata newMetadata = new ImageMetadata();
                    newMetadata.setId(UUID.randomUUID().toString());
                    newMetadata.setS3Key(originalKey);
                    return newMetadata;
                });

        metadata.setThumbnailKey(key);
        metadata.setThumbnailUrl(generateUrl(key));
        imageMetadataRepository.save(metadata);
    }

    @Override
    public String getStorageType() {
        return "azure-blob";
    }

    @Override
    protected String generateUrl(String key) {
        return "/s3/view/" + key;
    }

    private String extractOriginalKey(String key) {
        String suffix = "_thumbnail";
        int suffixIndex = key.lastIndexOf(suffix);
        if (suffixIndex > 0) {
            // Preserve the file extension: "base_thumbnail.jpg" → "base.jpg"
            String ext = StorageUtil.getExtension(key);
            return key.substring(0, suffixIndex) + ext;
        }
        return key;
    }
}
