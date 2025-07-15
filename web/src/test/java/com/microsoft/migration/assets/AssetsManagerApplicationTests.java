package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.microsoft.migration.assets.config.TestServiceBusConfig;

@SpringBootTest(classes = {AssetsManagerApplication.class, TestServiceBusConfig.class})
@ActiveProfiles({"test", "dev"})
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
