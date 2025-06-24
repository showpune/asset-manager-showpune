package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
		"spring.autoconfigure.exclude=com.azure.spring.cloud.autoconfigure.implementation.servicebus.AzureServiceBusAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
		// This test simply verifies that the application context can be loaded
		// without external dependencies like Azure Service Bus or Database
	}

}
