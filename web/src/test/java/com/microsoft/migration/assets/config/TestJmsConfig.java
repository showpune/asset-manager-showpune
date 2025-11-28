package com.microsoft.migration.assets.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestJmsConfig {
    
    @Bean
    public JmsTemplate jmsTemplate() {
        return mock(JmsTemplate.class);
    }
}
