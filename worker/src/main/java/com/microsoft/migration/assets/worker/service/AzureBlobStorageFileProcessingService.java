package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class AzureBlobStorageFileProcessingService extends AbstractFileProcessingService {

    private static final String THUMBNAIL_SUFFIX = "_thumbnail";

    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        try (InputStream inputStream = blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(key)
                .openInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);

        try (InputStream is = Files.newInputStream(source)) {
            blobClient.upload(is, Files.size(source), true);
        }
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));

        ImageMetadata metadata = imageMetadataRepository.findById(extractOriginalKey(key))
                .orElseGet(() -> {
                    ImageMetadata newMetadata = new ImageMetadata();
                    newMetadata.setId(extractOriginalKey(key));
                    return newMetadata;
                });

        metadata.setThumbnailKey(key);
        metadata.setThumbnailUrl(generateUrl(key));
        imageMetadataRepository.save(metadata);
    }

    @Override
    public String getStorageType() {
        return "blob";
    }

    @Override
    protected String generateUrl(String key) {
        return blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(key)
                .getBlobUrl();
    }

    private String extractOriginalKey(String key) {
        int suffixIndex = key.lastIndexOf(THUMBNAIL_SUFFIX);
        if (suffixIndex > 0) {
            return key.substring(0, suffixIndex);
        }
        return key;
    }
}
