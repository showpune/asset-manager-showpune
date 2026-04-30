package com.microsoft.migration.assets.baseline;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test configuration that disables RabbitMQ and AWS S3 auto-configuration
 * so baseline tests can run without any real external services.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
public class BaselineTestConfig {
}
