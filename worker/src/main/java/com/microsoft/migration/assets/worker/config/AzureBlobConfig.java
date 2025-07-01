package com.microsoft.migration.assets.worker.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!dev") // Active when not in dev profile
public class AzureBlobConfig {

    @Value("${azure.storage.endpoint}")
    private String storageEndpoint;

    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .endpoint(storageEndpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
}