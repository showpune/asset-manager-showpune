package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;

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

    /**
     * Processes image messages from a backup queue for monitoring and resilience purposes.
     * Uses the same JMS API pattern as the worker module.
     */
    @JmsListener(destination = QUEUE_NAME)
    public void processBackupMessage(final ImageProcessingMessage message, Message jmsMessage) {
        try {
            log.info("[BACKUP] Monitoring message: {}", message.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    message.getContentType(), message.getStorageType(), message.getSize());
            
            // Acknowledge the message
            jmsMessage.acknowledge();
            log.info("[BACKUP] Successfully processed message: {}", message.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + message.getKey(), e);
            try {
                jmsMessage.acknowledge();
            } catch (JMSException ackEx) {
                log.error("[BACKUP] Error handling JMS acknowledgment: {}", message.getKey(), ackEx);
            }
        }
    }
}