package com.microsoft.migration.assets.worker.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.migration.assets.worker.model.ImageMetadata;
import com.microsoft.migration.assets.worker.model.ImageProcessingMessage;
import com.microsoft.migration.assets.worker.repository.ImageMetadataRepository;
import com.microsoft.migration.assets.worker.util.StorageUtil;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Profile("azure")
@RequiredArgsConstructor
@Slf4j
public class AzureBlobFileProcessingService implements FileProcessor {
    
    private final BlobServiceClient blobServiceClient;
    private final ImageMetadataRepository imageMetadataRepository;
    
    @Autowired
    private RetryTemplate retryTemplate;
    
    @Value("${azure.storage.container-name}")
    private String containerName;

    @Value("${azure.servicebus.queue-name}")
    private String queueName;

    @JmsListener(destination = "${azure.servicebus.queue-name}")
    public void processImage(final ImageProcessingMessage message, Message jmsMessage) {
        try {
            retryTemplate.execute(new RetryCallback<Void, Exception>() {
                @Override
                public Void doWithRetry(RetryContext context) throws Exception {
                    if (context.getRetryCount() > 0) {
                        log.info("Retry attempt {} for image: {}", context.getRetryCount(), message.getKey());
                    }
                    
                    processImageWithRetry(message);
                    return null;
                }
            });
            
            // Success - message will be auto-acknowledged
            log.info("Successfully processed and acknowledged message: {}", message.getKey());
        } catch (Exception e) {
            log.error("All retry attempts failed for image: " + message.getKey(), e);
            // Message will be redelivered according to Service Bus retry policy
            throw new RuntimeException("Failed to process image after retries: " + message.getKey(), e);
        }
    }
    
    private void processImageWithRetry(ImageProcessingMessage message) {
        Path tempDir = null;
        Path originalFile = null;
        Path thumbnailFile = null;

        try {
            log.info("Processing image: {}", message.getKey());

            tempDir = Files.createTempDirectory("image-processing");
            originalFile = tempDir.resolve("original" + StorageUtil.getExtension(message.getKey()));
            thumbnailFile = tempDir.resolve("thumbnail" + StorageUtil.getExtension(message.getKey()));

            // Only process if message matches our storage type
            if (message.getStorageType().equals(getStorageType())) {
                // Download original file
                downloadOriginal(message.getKey(), originalFile);

                // Generate thumbnail
                generateThumbnail(originalFile, thumbnailFile);

                // Upload thumbnail
                String thumbnailKey = StorageUtil.getThumbnailKey(message.getKey());
                uploadThumbnail(thumbnailFile, thumbnailKey, message.getContentType());

                log.info("Successfully processed image: {}", message.getKey());
            } else {
                log.debug("Skipping message with storage type: {} (we handle {})",
                    message.getStorageType(), getStorageType());
            }
        } catch (Exception e) {
            log.error("Failed to process image: " + message.getKey(), e);
            throw new RuntimeException("Failed to process image: " + message.getKey(), e);
        } finally {
            try {
                // Cleanup temporary files
                if (originalFile != null) {
                    Files.deleteIfExists(originalFile);
                }
                if (thumbnailFile != null) {
                    Files.deleteIfExists(thumbnailFile);
                }
                if (tempDir != null) {
                    Files.deleteIfExists(tempDir);
                }
            } catch (IOException e) {
                log.error("Error cleaning up temporary files for: {}", message.getKey(), e);
            }
        }
    }

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        try (var inputStream = blobClient.openInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        
        blobClient.uploadFromFile(source.toString(), true);
        
        if (contentType != null) {
            blobClient.setHttpHeaders(new com.azure.storage.blob.models.BlobHttpHeaders()
                    .setContentType(contentType));
        }
        
        // Save or update thumbnail metadata
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

    protected String generateUrl(String key) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(key);
        return blobClient.getBlobUrl();
    }

    private String extractOriginalKey(String key) {
        String suffix = "_thumbnail";
        int index = key.indexOf(suffix);
        if (index > 0) {
            // Remove the suffix and extension, then re-add just the extension
            String withoutSuffix = key.substring(0, index);
            String extension = StorageUtil.getExtension(key);
            return withoutSuffix + extension;
        }
        return key;
    }

    protected void generateThumbnail(Path input, Path output) throws IOException {
        log.info("Generating thumbnail for: {}", input);

        BufferedImage originalImage = ImageIO.read(input.toFile());
        if (originalImage == null) {
            throw new IOException("Could not read image file: " + input);
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int maxDimension = 600;
        int thumbnailWidth, thumbnailHeight;
        
        double aspectRatio = (double) originalWidth / originalHeight;

        if (originalWidth > originalHeight) {
            thumbnailWidth = maxDimension;
            thumbnailHeight = (int) (maxDimension / aspectRatio);
        } else {
            thumbnailHeight = maxDimension;
            thumbnailWidth = (int) (maxDimension * aspectRatio);
        }

        BufferedImage resultImage = progressiveScaling(originalImage, thumbnailWidth, thumbnailHeight);
        resultImage = sharpenImage(resultImage);

        String extension = StorageUtil.getExtension(output.toString());
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        if (extension.isEmpty()) {
            extension = "jpg";
        }

        if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
            javax.imageio.ImageWriter jpgWriter = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
            javax.imageio.ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
            jpgWriteParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            jpgWriteParam.setCompressionQuality(0.95f);
            
            javax.imageio.IIOImage outputImage = new javax.imageio.IIOImage(resultImage, null, null);
            javax.imageio.stream.ImageOutputStream outputStream = 
                javax.imageio.ImageIO.createImageOutputStream(output.toFile());
            jpgWriter.setOutput(outputStream);
            jpgWriter.write(null, outputImage, jpgWriteParam);
            jpgWriter.dispose();
            outputStream.close();
        } else {
            ImageIO.write(resultImage, extension, output.toFile());
        }

        log.info("Successfully generated thumbnail: {}", output);
    }
    
    private BufferedImage progressiveScaling(BufferedImage source, int targetWidth, int targetHeight) {
        int currentWidth = source.getWidth();
        int currentHeight = source.getHeight();
        
        if (currentWidth <= targetWidth && currentHeight <= targetHeight) {
            return source;
        }
        
        BufferedImage result = source;
        
        while (currentWidth > targetWidth * 1.5 || currentHeight > targetHeight * 1.5) {
            int newWidth = Math.max(currentWidth / 2, targetWidth);
            int newHeight = Math.max(currentHeight / 2, targetHeight);
            
            result = scaleImage(result, newWidth, newHeight);
            
            currentWidth = newWidth;
            currentHeight = newHeight;
        }
        
        if (currentWidth != targetWidth || currentHeight != targetHeight) {
            result = scaleImage(result, targetWidth, targetHeight);
        }
        
        return result;
    }
    
    private BufferedImage scaleImage(BufferedImage source, int width, int height) {
        BufferedImage result;
        
        if (source.getTransparency() != BufferedImage.OPAQUE) {
            result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(source, 0, 0, width, height, null);
        g2d.dispose();
        
        return result;
    }
    
    private BufferedImage sharpenImage(BufferedImage image) {
        float[] sharpenMatrix = {
            0, -0.2f, 0,
            -0.2f, 1.8f, -0.2f,
            0, -0.2f, 0
        };
        
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(3, 3, sharpenMatrix);
        java.awt.image.ConvolveOp convolveOp = new java.awt.image.ConvolveOp(
            kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage output;
        if (image.getTransparency() != BufferedImage.OPAQUE) {
            output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        } else {
            output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB); 
        }
        
        return convolveOp.filter(image, output);
    }
}
