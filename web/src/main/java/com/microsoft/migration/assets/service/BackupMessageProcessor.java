package com.microsoft.migration.assets.service;

import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
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
     * Uses Azure Service Bus API pattern as the worker module.
     */
    @ServiceBusListener(destination = QUEUE_NAME)
    public void processBackupMessage(ImageProcessingMessage messagePayload) {
        try {
            log.info("[BACKUP] Monitoring message: {}", messagePayload.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    messagePayload.getContentType(), messagePayload.getStorageType(), messagePayload.getSize());
            
            log.info("[BACKUP] Successfully processed message: {}", messagePayload.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + messagePayload.getKey(), e);
            // Re-throw to let Service Bus handle retry logic
            throw e;
        }
    }
}