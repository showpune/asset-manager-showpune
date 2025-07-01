package com.microsoft.migration.assets.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class StorageServiceProfileTest {

    @Autowired
    private StorageService storageService;

    @Test
    void testLocalStorageServiceIsLoadedInDevProfile() {
        // In dev profile, LocalFileStorageService should be loaded
        assertNotNull(storageService);
        assertEquals("local", storageService.getStorageType());
        assertTrue(storageService instanceof LocalFileStorageService);
    }
}