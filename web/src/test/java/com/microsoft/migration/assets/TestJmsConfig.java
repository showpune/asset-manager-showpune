package com.microsoft.migration.assets;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.jms.ConnectionFactory;

/**
 * Provides a mock JMS ConnectionFactory for unit tests so the Spring context
 * can load without a real Azure Service Bus connection.
 */
@TestConfiguration
public class TestJmsConfig {

    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        return Mockito.mock(ConnectionFactory.class);
    }
}
