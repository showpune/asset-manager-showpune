package com.microsoft.migration.assets.worker.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties.AzureServiceBusProperties;
import com.azure.spring.messaging.implementation.annotation.EnableAzureMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableAzureMessaging
public class ServiceBusConfig {
    public static final String QUEUE_NAME = "image-processing";
    public static final int RETRY_DELAY_MS = 60000; // 1 minute delay
    public static final int MAX_ATTEMPTS = 3; // Maximum number of retry attempts

    @Bean
    public ServiceBusAdministrationClient adminClient(AzureServiceBusProperties properties, TokenCredential credential) {
        return new ServiceBusAdministrationClientBuilder()
                .credential(properties.getFullyQualifiedNamespace(), credential)
                .buildClient();
    }

    @Bean
    public QueueProperties queue(ServiceBusAdministrationClient adminClient) {
        try {
            return adminClient.getQueue(QUEUE_NAME);
        } catch (ResourceNotFoundException e) {
            return adminClient.createQueue(QUEUE_NAME);
        }
    }
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure retry policy (number of attempts)
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure backoff policy (delay between retries)
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(RETRY_DELAY_MS);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}
