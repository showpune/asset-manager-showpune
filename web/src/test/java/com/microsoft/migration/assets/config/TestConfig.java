package com.microsoft.migration.assets.config;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public ServiceBusTemplate serviceBusTemplate() {
        return Mockito.mock(ServiceBusTemplate.class);
    }
}