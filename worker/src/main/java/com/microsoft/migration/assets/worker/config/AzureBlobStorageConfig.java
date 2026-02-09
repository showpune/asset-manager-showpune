package com.microsoft.migration.assets.worker.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobStorageConfig {
    @Value("${azure.storage.account-name}")
    private String accountName;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Bean
    public BlobServiceClient blobServiceClient() {
        String endpoint = String.format("https://%s.blob.core.windows.net", accountName);
        
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    public String containerName() {
        return containerName;
    }
}