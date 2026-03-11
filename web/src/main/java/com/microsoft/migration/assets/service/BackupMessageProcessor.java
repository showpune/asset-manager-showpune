package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.microsoft.migration.assets.config.RabbitConfig.QUEUE_NAME;

import java.io.IOException;

@Component
@Profile("backup") 
public class BackupMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(BackupMessageProcessor.class);

    @RabbitListener(queues = QUEUE_NAME)
    public void processBackupMessage(final ImageProcessingMessage message, 
                                    Channel channel, 
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("[BACKUP] Monitoring message: {}", message.getKey());
            log.info("[BACKUP] Content type: {}, Storage: {}, Size: {}", 
                    message.getContentType(), message.getStorageType(), message.getSize());
            
            channel.basicAck(deliveryTag, false);
            log.info("[BACKUP] Successfully processed message: {}", message.getKey());
        } catch (Exception e) {
            log.error("[BACKUP] Failed to process message: " + message.getKey(), e);
            
            try {
                channel.basicNack(deliveryTag, false, true);
                log.warn("[BACKUP] Message requeued: {}", message.getKey());
            } catch (IOException ackEx) {
                log.error("[BACKUP] Error handling RabbitMQ acknowledgment: {}", message.getKey(), ackEx);
            }
        }
    }
}