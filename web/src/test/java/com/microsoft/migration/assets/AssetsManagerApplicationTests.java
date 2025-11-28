package com.microsoft.migration.assets;

import com.microsoft.migration.assets.config.TestJmsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Import(TestJmsConfig.class)
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
