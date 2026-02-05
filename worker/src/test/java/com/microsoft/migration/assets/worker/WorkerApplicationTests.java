package com.microsoft.migration.assets.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

@SpringBootTest
@ContextConfiguration(classes = WorkerApplicationTests.TestConfig.class)
class WorkerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Configuration
	static class TestConfig {
		@Bean
		@Primary
		public ConnectionFactory connectionFactory() {
			// Return a dummy connection factory for tests
			return new CachingConnectionFactory();
		}
	}

}
