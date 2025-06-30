package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Azure Blob Storage.
 * This test only runs when AZURE_STORAGE_ACCOUNT_ENDPOINT environment variable is set.
 * 
 * To run this test with Azure:
 * 1. Set AZURE_STORAGE_ACCOUNT_ENDPOINT=https://yourstorageaccount.blob.core.windows.net
 * 2. Set azure.storage.container.name=your-container-name in application.properties
 * 3. Ensure Azure credentials are configured (Azure CLI login or managed identity)
 * 4. Run: mvn test -Dspring.profiles.active=prod
 */
@SpringBootTest
@ActiveProfiles("prod")
@Import(TestConfig.class)
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_ENDPOINT", matches = ".*")
class AzureBlobStorageIntegrationTest {

    @Autowired
    private StorageService storageService;

    @Test
    void shouldUseAzureBlobStorageInProdProfile() {
        // In prod profile (when not dev), AzureBlobStorageService should be used
        assertThat(storageService.getStorageType()).isEqualTo("azure");
    }

    @Test
    void shouldListObjectsFromAzureWithoutError() {
        // Basic test to ensure listObjects works with Azure Blob Storage
        var objects = storageService.listObjects();
        assertThat(objects).isNotNull();
        // The list may be empty if no objects exist in the container, but should not fail
    }
}