package com.microsoft.migration.assets;

import com.microsoft.migration.assets.config.ServiceBusTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(ServiceBusTestConfig.class)
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
