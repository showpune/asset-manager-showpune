package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev") // Use dev profile to avoid database connection
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
