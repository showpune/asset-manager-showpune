package com.microsoft.migration.assets.config;

import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties.AzureServiceBusProperties;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestServiceBusConfig {

    @Bean
    @Primary
    public ServiceBusTemplate serviceBusTemplate() {
        return Mockito.mock(ServiceBusTemplate.class);
    }

    @Bean
    @Primary
    public ServiceBusAdministrationClient adminClient() {
        return Mockito.mock(ServiceBusAdministrationClient.class);
    }

    @Bean
    @Primary
    public QueueProperties imageProcessingQueue() {
        return Mockito.mock(QueueProperties.class);
    }

    @Bean
    @Primary
    public AzureServiceBusProperties azureServiceBusProperties() {
        return Mockito.mock(AzureServiceBusProperties.class);
    }

    @Bean
    @Primary
    public TokenCredential tokenCredential() {
        return Mockito.mock(TokenCredential.class);
    }
}