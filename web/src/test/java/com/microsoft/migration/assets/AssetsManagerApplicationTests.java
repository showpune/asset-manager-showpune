package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.azure.spring.cloud.autoconfigure.implementation.servicebus.AzureServiceBusAutoConfiguration;

@SpringBootTest(exclude = AzureServiceBusAutoConfiguration.class)
@ActiveProfiles("test")
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
