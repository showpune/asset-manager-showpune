package com.microsoft.migration.assets.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String QUEUE_NAME = "image-processing";
}
