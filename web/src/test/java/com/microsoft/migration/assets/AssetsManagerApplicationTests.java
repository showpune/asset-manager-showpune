package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.microsoft.migration.assets.config.TestServiceBusConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestServiceBusConfig.class)
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
