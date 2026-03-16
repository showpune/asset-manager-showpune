package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

@SpringBootTest
class AssetsManagerApplicationTests {

	@MockBean
	ConnectionFactory connectionFactory;

	@MockBean
	JmsTemplate jmsTemplate;

	@Test
	void contextLoads() {
	}

}
