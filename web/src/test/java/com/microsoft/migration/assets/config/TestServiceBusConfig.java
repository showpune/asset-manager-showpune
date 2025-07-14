package com.microsoft.migration.assets.config;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestServiceBusConfig {
    
    @Bean
    @Primary
    public ServiceBusTemplate serviceBusTemplate() {
        return mock(ServiceBusTemplate.class);
    }
}