package com.microsoft.migration.assets.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
public class StorageServiceProfileTest {

    @Autowired
    private StorageService storageService;

    @Test
    public void testDevProfileUsesLocalStorage() {
        assertNotNull(storageService);
        assertEquals("local", storageService.getStorageType());
    }
}