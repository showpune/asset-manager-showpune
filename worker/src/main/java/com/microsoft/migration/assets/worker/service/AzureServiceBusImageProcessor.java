package com.microsoft.migration.assets.worker.service;

import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.microsoft.migration.assets.worker.model.ImageProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("azure")
@RequiredArgsConstructor
@Slf4j
public class AzureServiceBusImageProcessor {

    private final AzureBlobFileProcessingService fileProcessingService;

    @ServiceBusListener(destination = "${azure.servicebus.queue.name}")
    public void processImageMessage(ImageProcessingMessage message) {
        try {
            log.info("Received Azure Service Bus message for processing image: {}", message.getKey());
            
            // Only process if message matches our storage type
            if (message.getStorageType().equals(fileProcessingService.getStorageType())) {
                fileProcessingService.processImageMessage(message);
                log.info("Successfully processed Azure Service Bus message for image: {}", message.getKey());
            } else {
                log.debug("Skipping message with storage type: {} (we handle {})",
                    message.getStorageType(), fileProcessingService.getStorageType());
            }
        } catch (Exception e) {
            log.error("Failed to process Azure Service Bus message for image: {}", message.getKey(), e);
            throw new RuntimeException("Failed to process image: " + message.getKey(), e);
        }
    }
}