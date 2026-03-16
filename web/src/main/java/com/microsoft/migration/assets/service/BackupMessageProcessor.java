package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static com.microsoft.migration.assets.config.ServiceBusJmsConfig.QUEUE_NAME;

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
     */
    @JmsListener(destination = QUEUE_NAME)
    public void processBackupMessage(final ImageProcessingMessage message) {
        log.info("[BACKUP] Monitoring message: {}", message.getKey());
        log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                message.getContentType(), message.getStorageType(), message.getSize());
        log.info("[BACKUP] Successfully processed message: {}", message.getKey());
    }
}
