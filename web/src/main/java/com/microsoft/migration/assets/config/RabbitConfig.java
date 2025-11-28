package com.microsoft.migration.assets.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties.AzureServiceBusProperties;
import com.azure.spring.messaging.ConsumerIdentifier;
import com.azure.spring.messaging.PropertiesSupplier;
import com.azure.spring.messaging.servicebus.core.properties.ProcessorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String QUEUE_NAME = "image-processing";

    @Bean
    public ServiceBusAdministrationClient adminClient(AzureServiceBusProperties properties, TokenCredential credential) {
        return new ServiceBusAdministrationClientBuilder()
                .credential(properties.getFullyQualifiedNamespace(), credential)
                .buildClient();
    }

    @Bean
    public QueueProperties imageProcessingQueue(ServiceBusAdministrationClient adminClient) {
        try {
            return adminClient.getQueue(QUEUE_NAME);
        } catch (ResourceNotFoundException e) {
            return adminClient.createQueue(QUEUE_NAME);
        }
    }

    @Bean
    public PropertiesSupplier<ConsumerIdentifier, ProcessorProperties> processorPropertiesSupplier() {
        return key -> {
            ProcessorProperties properties = new ProcessorProperties();
            properties.setAutoComplete(false);
            return properties;
        };
    }
}
