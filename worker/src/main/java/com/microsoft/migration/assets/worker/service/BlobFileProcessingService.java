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
public class BlobFileProcessingService extends AbstractFileProcessingService {

    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    private BlobContainerClient getContainerClient() {
        return blobServiceClient.getBlobContainerClient(containerName);
    }

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobClient blobClient = getContainerClient().getBlobClient(key);
        try (InputStream inputStream = blobClient.openInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobClient blobClient = getContainerClient().getBlobClient(key);
        blobClient.uploadFromFile(source.toString(), true);
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
        return getContainerClient().getBlobClient(key).getBlobUrl();
    }

    private String extractOriginalKey(String key) {
        String suffix = "_thumbnail";
        int suffixIndex = key.lastIndexOf(suffix);
        if (suffixIndex > 0) {
            return key.substring(0, suffixIndex);
        }
        return key;
    }
}
