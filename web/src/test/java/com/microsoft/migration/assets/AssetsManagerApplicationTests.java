package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // Use test profile to disable Service Bus and other production services
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
		// Test that Spring context loads successfully with production services disabled for test
	}

}
