package com.microsoft.migration.assets;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class AssetsManagerApplicationTests {

	@MockBean
	ServiceBusTemplate serviceBusTemplate;

	@MockBean
	ServiceBusAdministrationClient serviceBusAdministrationClient;

	@Test
	void contextLoads() {
	}

}
