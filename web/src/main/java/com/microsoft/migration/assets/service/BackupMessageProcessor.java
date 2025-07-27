package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.microsoft.migration.assets.config.ServiceBusConfig.QUEUE_NAME;

/**
 * A backup message processor that serves as a monitoring and logging service.
 * 
 * Only enabled when the "backup" profile is active.
 */
@Slf4j
@Component
@Profile("backup") 
public class BackupMessageProcessor {

    /**
     * Processes image messages from a backup queue for monitoring and resilience purposes.
     * Uses Azure Service Bus listener.
     */
    @ServiceBusListener(destination = QUEUE_NAME)
    public void processBackupMessage(ImageProcessingMessage payload) {
        try {
            log.info("[BACKUP] Monitoring message: {}", payload.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    payload.getContentType(), payload.getStorageType(), payload.getSize());
            
            log.info("[BACKUP] Successfully processed message: {}", payload.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + payload.getKey(), e);
            // Re-throw to trigger Service Bus retry mechanism
            throw new RuntimeException("Backup processing failed for: " + payload.getKey(), e);
        }
    }
}