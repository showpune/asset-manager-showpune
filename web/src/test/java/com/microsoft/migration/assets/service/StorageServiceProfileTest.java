package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that the correct storage service is loaded based on Spring profiles.
 * This validates the profile-based dependency injection works correctly.
 */
@SpringBootTest
@Import(TestConfig.class)
class StorageServiceProfileTest {

    @Test
    @ActiveProfiles("dev")
    void shouldLoadLocalFileStorageServiceInDevProfile() {
        // This test runs with dev profile and should use LocalFileStorageService
        StorageServiceProfileTestContext testContext = new StorageServiceProfileTestContext();
        assertThat(testContext.getStorageType()).isEqualTo("local");
    }

    @SpringBootTest
    @Import(TestConfig.class)
    @ActiveProfiles("dev")
    static class StorageServiceProfileTestContext {
        @Autowired
        private StorageService storageService;

        public String getStorageType() {
            return storageService.getStorageType();
        }
    }
}