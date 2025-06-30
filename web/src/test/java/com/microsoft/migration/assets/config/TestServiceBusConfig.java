package com.microsoft.migration.assets.config;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties.AzureServiceBusProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestServiceBusConfig {

    @Bean
    @Primary
    public ServiceBusTemplate serviceBusTemplate() {
        return mock(ServiceBusTemplate.class);
    }

    @Bean
    @Primary
    public ServiceBusAdministrationClient serviceBusAdminClient() {
        return mock(ServiceBusAdministrationClient.class);
    }

    @Bean
    @Primary
    public TokenCredential tokenCredential() {
        return mock(TokenCredential.class);
    }

    @Bean
    @Primary
    public AzureServiceBusProperties azureServiceBusProperties() {
        return mock(AzureServiceBusProperties.class);
    }
}