package com.microsoft.migration.assets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.profiles.active=dev"
})
class AzureMigrationTest {

    @Test
    void contextLoadsWithDevProfile() {
        // This test verifies that the application context loads correctly
        // with the dev profile (using local file storage)
    }
}