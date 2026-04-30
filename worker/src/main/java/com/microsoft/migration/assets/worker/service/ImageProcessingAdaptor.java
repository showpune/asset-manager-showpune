package com.microsoft.migration.assets.worker.service;

import com.microsoft.migration.assets.worker.model.ImageProcessingMessage;

/**
 * Adaptor interface for image processing logic, decoupled from the messaging transport layer.
 * Both the old (RabbitMQ) and new (Azure Service Bus) implementations delegate to this interface.
 */
public interface ImageProcessingAdaptor {
    void process(ImageProcessingMessage message) throws Exception;
}
