package com.microsoft.migration.assets.worker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File processing service that uses mounted storage (local or Azure Storage File Share).
 * 
 * This service supports Azure Storage File Share when mounted to the container.
 * Configure the mount path using the 'storage.mount.path' property:
 * - Local development: storage.mount.path=../storage
 * - Azure Container Apps: storage.mount.path=/mnt/azure-storage
 * 
 * For Azure Container Apps, mount an Azure Storage File Share to the container
 * and configure the mount path accordingly.
 */
@Service
@Profile("dev")
public class LocalFileProcessingService extends AbstractFileProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalFileProcessingService.class);
    
    @Value("${storage.mount.path:${local.storage.directory:../storage}}")
    private String storageDirectory;
    
    private Path rootLocation;
    
    @PostConstruct
    public void init() throws Exception {
        rootLocation = Paths.get(storageDirectory).toAbsolutePath().normalize();
        logger.info("Local storage directory: {}", rootLocation);
        
        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
            logger.info("Created local storage directory");
        }
    }

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        Path sourcePath = rootLocation.resolve(key);
        if (!Files.exists(sourcePath)) {
            throw new java.io.FileNotFoundException("File not found: " + sourcePath);
        }
        Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        Path destinationPath = rootLocation.resolve(key);
        Files.createDirectories(destinationPath.getParent());
        Files.copy(source, destinationPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    protected String generateUrl(String key) {
        // For local storage, we'll just return the relative path
        return "/storage/" + key;
    }
}