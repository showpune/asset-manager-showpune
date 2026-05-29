package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@Profile("!dev")
@RequiredArgsConstructor
public class S3FileProcessingService extends AbstractFileProcessingService {

    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;

    @Value("${azure.storage.blob.container-name}")
    private String containerName;

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(key)
                .downloadToFile(destination.toString(), true);
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key);
        blobClient.uploadFromFileWithResponse(
                new BlobUploadFromFileOptions(source.toString())
                        .setHeaders(new BlobHttpHeaders().setContentType(contentType)),
                null, null);

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
        return "azure-blob";
    }

    @Override
    protected String generateUrl(String key) {
        return blobServiceClient.getBlobContainerClient(containerName).getBlobClient(key).getBlobUrl();
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