package com.microsoft.migration.assets.service;

import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
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
     * Uses the Azure Service Bus API pattern.
     */
    @ServiceBusListener(destination = QUEUE_NAME)
    public void processBackupMessage(final ImageProcessingMessage message, 
                                    Message<ImageProcessingMessage> serviceBusMessage,
                                    @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) Object context) {
        try {
            log.info("[BACKUP] Monitoring message: {}", message.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    message.getContentType(), message.getStorageType(), message.getSize());
            
            // Acknowledge the message by completing context
            if (context instanceof com.azure.messaging.servicebus.ServiceBusReceivedMessageContext) {
                ((com.azure.messaging.servicebus.ServiceBusReceivedMessageContext) context).complete();
            }
            log.info("[BACKUP] Successfully processed message: {}", message.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + message.getKey(), e);
            
            try {
                // Reject the message and requeue it by abandoning
                if (context instanceof com.azure.messaging.servicebus.ServiceBusReceivedMessageContext) {
                    ((com.azure.messaging.servicebus.ServiceBusReceivedMessageContext) context).abandon();
                }
                log.warn("[BACKUP] Message abandoned: {}", message.getKey());
            } catch (Exception ackEx) {
                log.error("[BACKUP] Error handling Service Bus acknowledgment: {}", message.getKey(), ackEx);
            }
        }
    }
}