package com.microsoft.migration.assets.config;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestServiceBusConfig {

    @MockBean
    private ServiceBusTemplate serviceBusTemplate;

}