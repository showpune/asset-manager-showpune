package com.microsoft.migration.assets.config;

import com.azure.spring.messaging.ConsumerIdentifier;
import com.azure.spring.messaging.PropertiesSupplier;
import com.azure.spring.messaging.implementation.annotation.EnableAzureMessaging;
import com.azure.spring.messaging.servicebus.core.properties.ProcessorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAzureMessaging
public class ServiceBusConfig {
    public static final String QUEUE_NAME = "image-processing";

    @Bean
    public PropertiesSupplier<ConsumerIdentifier, ProcessorProperties> serviceBusProcessorPropertiesSupplier() {
        return key -> {
            ProcessorProperties processorProperties = new ProcessorProperties();
            processorProperties.setAutoComplete(false);
            return processorProperties;
        };
    }
}
