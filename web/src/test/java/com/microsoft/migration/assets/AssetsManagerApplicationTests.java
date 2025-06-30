package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {AssetsManagerApplication.class})
@ActiveProfiles("test")
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring Boot application can start
		// The actual Service Bus beans won't be created due to disabled configuration
	}

}
