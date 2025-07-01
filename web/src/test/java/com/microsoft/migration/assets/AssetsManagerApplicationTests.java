package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {AssetsManagerApplicationTests.TestConfiguration.class})
@ActiveProfiles("test")
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

	@org.springframework.boot.test.context.TestConfiguration
	@ComponentScan(
		basePackages = "com.microsoft.migration.assets",
		excludeFilters = {
			@ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE, 
				classes = {
					com.microsoft.migration.assets.config.ServiceBusConfig.class,
					com.microsoft.migration.assets.service.BackupMessageProcessor.class
				}
			)
		}
	)
	static class TestConfiguration {
	}

}
