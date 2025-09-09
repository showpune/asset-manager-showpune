package com.microsoft.migration.assets;

import com.microsoft.migration.assets.config.ServiceBusConfig;
import com.microsoft.migration.assets.config.TestServiceBusConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {AssetsManagerApplication.class, TestServiceBusConfig.class},
                excludeClasses = {ServiceBusConfig.class})
@ActiveProfiles("test")
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
