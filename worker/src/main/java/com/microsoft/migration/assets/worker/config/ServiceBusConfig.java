package com.microsoft.migration.assets.worker.config;

import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
@Profile("azure")
public class ServiceBusConfig {

    @Value("${azure.servicebus.namespace}")
    private String namespace;

    @Bean
    public ConnectionFactory connectionFactory() {
        // Azure Service Bus connection string for managed identity
        String connectionString = String.format(
            "amqps://%s.servicebus.windows.net",
            namespace
        );
        
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory();
        jmsConnectionFactory.setRemoteURI(connectionString);
        
        return jmsConnectionFactory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }
}
