package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;

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
     * Uses Azure Service Bus API pattern.
     */
    @ServiceBusListener(destination = QUEUE_NAME)
    public void processBackupMessage(ImageProcessingMessage payload, 
                                   Message<ImageProcessingMessage> message,
                                   @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context) {
        try {
            log.info("[BACKUP] Monitoring message: {}", payload.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    payload.getContentType(), payload.getStorageType(), payload.getSize());
            
            // Complete the message
            context.complete();
            log.info("[BACKUP] Successfully processed message: {}", payload.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + payload.getKey(), e);
            
            try {
                // Abandon the message to requeue it
                context.abandon();
                log.warn("[BACKUP] Message abandoned: {}", payload.getKey());
            } catch (Exception ackEx) {
                log.error("[BACKUP] Error handling Service Bus acknowledgment: {}", payload.getKey(), ackEx);
            }
        }
    }
}