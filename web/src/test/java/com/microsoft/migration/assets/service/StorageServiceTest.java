package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Import(TestConfig.class)
class StorageServiceTest {

    @Autowired
    private StorageService storageService;

    @Test
    void contextLoads() {
        // Verify that StorageService bean is created
        assertThat(storageService).isNotNull();
    }

    @Test
    void shouldUseLocalFileStorageInDevProfile() {
        // In dev profile, LocalFileStorageService should be used
        assertThat(storageService.getStorageType()).isEqualTo("local");
    }

    @Test
    void shouldListObjectsWithoutError() {
        // Basic test to ensure listObjects doesn't throw an exception
        var objects = storageService.listObjects();
        assertThat(objects).isNotNull();
    }
}