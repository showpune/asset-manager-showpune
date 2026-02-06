package com.microsoft.migration.assets.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("azure")
public class AzureBlobConfig {

    @Value("${azure.storage.account-name}")
    private String storageAccountName;

    @Value("${azure.storage.container-name:images}")
    private String containerName;

    @Bean
    public DefaultAzureCredential defaultAzureCredential() {
        return new DefaultAzureCredentialBuilder()
                .build();
    }

    @Bean
    public BlobServiceClient blobServiceClient(DefaultAzureCredential credential) {
        String endpoint = String.format("https://%s.blob.core.windows.net", storageAccountName);
        
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
    }

    @Bean
    public String containerName() {
        return containerName;
    }
}
