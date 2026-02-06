package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@EnableAutoConfiguration(exclude = {
    com.azure.spring.cloud.autoconfigure.implementation.jms.ServiceBusJmsAutoConfiguration.class
})
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
