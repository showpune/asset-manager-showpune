package com.microsoft.migration.assets.config;

import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
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
    public TokenCredential mockTokenCredential() {
        return Mockito.mock(TokenCredential.class);
    }

    @Bean
    @Primary
    public ServiceBusAdministrationClient mockAdminClient() {
        return Mockito.mock(ServiceBusAdministrationClient.class);
    }

    @Bean
    @Primary
    public ServiceBusTemplate mockServiceBusTemplate() {
        return Mockito.mock(ServiceBusTemplate.class);
    }
}