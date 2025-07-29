package com.microsoft.migration.assets.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class to verify Azure Service Bus configuration loads correctly
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceBusConfigTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring context can load with our Azure Service Bus configuration
        // If the configuration has issues, this test will fail during context startup
    }
}