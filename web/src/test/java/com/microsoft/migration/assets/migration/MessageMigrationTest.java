package com.microsoft.migration.assets.migration;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that the migration from RabbitMQ to Azure Service Bus
 * maintains the same message structure and processing behavior.
 */
class MessageMigrationTest {

    @Test
    void imageProcessingMessage_shouldMaintainCompatibility() {
        // Given - Create a message like it would be created for RabbitMQ
        String key = "test-image.jpg";
        String contentType = "image/jpeg";
        String storageType = "local";
        long size = 1024L;

        // When - Create the message (this would work with both RabbitMQ and Azure Service Bus)
        ImageProcessingMessage message = new ImageProcessingMessage(key, contentType, storageType, size);

        // Then - Verify the message structure is intact
        assertEquals(key, message.getKey());
        assertEquals(contentType, message.getContentType());
        assertEquals(storageType, message.getStorageType());
        assertEquals(size, message.getSize());
    }

    @Test
    void messageProcessing_shouldSupportBothStorageTypes() {
        // Given - Messages for both storage types
        ImageProcessingMessage localMessage = new ImageProcessingMessage("local-image.jpg", "image/jpeg", "local", 1024L);
        ImageProcessingMessage s3Message = new ImageProcessingMessage("s3-key", "image/png", "s3", 2048L);

        // Then - Both message types should be valid
        assertNotNull(localMessage);
        assertNotNull(s3Message);
        
        // And - Storage types should be preserved for routing in the worker
        assertEquals("local", localMessage.getStorageType());
        assertEquals("s3", s3Message.getStorageType());
    }

    @Test
    void queueName_shouldRemainConsistent() {
        // Given - The queue name constant from the configuration
        String expectedQueueName = "image-processing";
        
        // When - Compare with what we'd expect from the web configuration
        String webQueueName = com.microsoft.migration.assets.config.ServiceBusConfig.QUEUE_NAME;
        
        // Then - Queue name should be consistent with expected value
        assertEquals(expectedQueueName, webQueueName);
    }
}