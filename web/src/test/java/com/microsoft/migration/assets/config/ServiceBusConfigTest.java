package com.microsoft.migration.assets.config;

import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties.AzureServiceBusProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = ServiceBusConfig.class)
@TestPropertySource(properties = {
    "spring.cloud.azure.credential.managed-identity-enabled=true",
    "spring.cloud.azure.credential.client-id=test-client-id",
    "spring.cloud.azure.servicebus.entity-type=queue",
    "spring.cloud.azure.servicebus.namespace=test-namespace"
})
public class ServiceBusConfigTest {

    @MockBean
    private TokenCredential tokenCredential;

    @MockBean 
    private AzureServiceBusProperties azureServiceBusProperties;

    @MockBean
    private ServiceBusAdministrationClient adminClient;

    @MockBean
    private QueueProperties queueProperties;

    @Test
    public void testQueueNameConstant() {
        assertEquals("image-processing", ServiceBusConfig.QUEUE_NAME);
    }
}