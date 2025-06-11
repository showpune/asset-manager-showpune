package com.microsoft.migration.assets.config;

import com.azure.spring.messaging.implementation.annotation.EnableAzureMessaging;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAzureMessaging
public class AzureServiceBusConfig {
    // The Azure Service Bus configuration is handled by Spring Cloud Azure auto-configuration
    // using the properties configured in application.properties
}