package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
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
     * Uses Azure Service Bus API pattern.
     */
    @ServiceBusListener(destination = QUEUE_NAME)
    public void processBackupMessage(ImageProcessingMessage messagePayload, 
                                    Message<ImageProcessingMessage> message,
                                    @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context) {
        try {
            log.info("[BACKUP] Monitoring message: {}", messagePayload.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    messagePayload.getContentType(), messagePayload.getStorageType(), messagePayload.getSize());
            
            // Complete the message
            context.complete();
            log.info("[BACKUP] Successfully processed message: {}", messagePayload.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + messagePayload.getKey(), e);
            
            try {
                // Abandon the message to requeue it
                context.abandon();
                log.warn("[BACKUP] Message abandoned: {}", messagePayload.getKey());
            } catch (Exception abandonEx) {
                log.error("[BACKUP] Error handling Service Bus message completion: {}", messagePayload.getKey(), abandonEx);
            }
        }
    }
}