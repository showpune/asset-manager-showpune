package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestJmsConfig.class)
class AssetsManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
