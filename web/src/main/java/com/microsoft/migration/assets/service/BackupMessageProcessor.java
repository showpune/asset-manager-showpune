package com.microsoft.migration.assets.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

/**
 * A backup message processor that serves as a monitoring and logging service.
 * 
 * Only enabled when the "backup" profile is active.
 */
@Slf4j
@Component
@Profile("backup") 
public class BackupMessageProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Processes image messages from a backup queue for monitoring and resilience purposes.
     * Uses JMS for Azure Service Bus.
     */
    @JmsListener(destination = QUEUE_NAME)
    public void processBackupMessage(final String messageJson) {
        try {
            ImageProcessingMessage message = objectMapper.readValue(messageJson, ImageProcessingMessage.class);
            log.info("[BACKUP] Monitoring message: {}", message.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    message.getContentType(), message.getStorageType(), message.getSize());
            
            log.info("[BACKUP] Successfully processed message: {}", message.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + messageJson, e);
        }
    }
}