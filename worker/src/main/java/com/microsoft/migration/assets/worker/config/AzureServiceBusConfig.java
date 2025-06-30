package com.microsoft.migration.assets.worker.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties.AzureServiceBusProperties;
import com.azure.spring.messaging.implementation.annotation.EnableAzureMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAzureMessaging
public class AzureServiceBusConfig {

    @Value("${azure.servicebus.queue.name}")
    private String queueName;

    @Bean
    public ServiceBusAdministrationClient adminClient(AzureServiceBusProperties properties) {
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        return new ServiceBusAdministrationClientBuilder()
                .credential(properties.getFullyQualifiedNamespace(), credential)
                .buildClient();
    }

    @Bean
    public QueueProperties queue(ServiceBusAdministrationClient adminClient) {
        try {
            return adminClient.getQueue(queueName);
        } catch (ResourceNotFoundException e) {
            return adminClient.createQueue(queueName);
        }
    }
}