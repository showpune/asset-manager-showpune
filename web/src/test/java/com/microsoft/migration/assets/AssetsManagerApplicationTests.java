package com.microsoft.migration.assets;

import com.microsoft.migration.assets.config.TestServiceBusConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestServiceBusConfig.class)
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
