package com.microsoft.migration.assets.worker.config;

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
    private String accountName;

    @Value("${azure.storage.connection-string:#{null}}")
    private String connectionString;

    @Bean
    public BlobServiceClient blobServiceClient() {
        // For production: Use managed identity (DefaultAzureCredential)
        // For development: Use connection string if provided
        if (connectionString != null && !connectionString.isEmpty()) {
            return new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        } else {
            // Use managed identity with DefaultAzureCredential
            String endpoint = String.format("https://%s.blob.core.windows.net", accountName);
            return new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        }
    }
}
