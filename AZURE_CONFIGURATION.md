# Azure Storage Account Configuration Example

This file provides an example of how to configure the Asset Manager application to use Azure Storage Account.

## Application Properties Configuration

### Web Module (web/src/main/resources/application.properties)
```properties
# Azure Storage Account Configuration
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=assets-container

# Max file size for uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/assets_manager
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true
```

### Worker Module (worker/src/main/resources/application.properties)
```properties
# Azure Storage Account Configuration
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=assets-container

# Server port (different from web module)
server.port=8081

# Application name
spring.application.name=assets-manager-worker

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/assets_manager
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## Azure Storage Account Setup

1. **Create Storage Account**: Create an Azure Storage Account in your Azure subscription
2. **Create Container**: Create a blob container named `assets-container` (or update the configuration above)
3. **Configure Authentication**: The application uses DefaultAzureCredential for authentication. Set up one of the following:

### Option 1: Managed Identity (Recommended for Production)
- Assign a managed identity to your Azure App Service or VM
- Grant the managed identity "Storage Blob Data Contributor" role on the storage account

### Option 2: Service Principal (Development/Testing)
Set environment variables:
```bash
export AZURE_CLIENT_ID=<your-client-id>
export AZURE_CLIENT_SECRET=<your-client-secret>
export AZURE_TENANT_ID=<your-tenant-id>
```

### Option 3: Azure CLI (Local Development)
Run `az login` to authenticate locally

## Environment Variables

For different environments, you can override the configuration using environment variables:

```bash
# Azure Storage Configuration
export AZURE_STORAGE_ENDPOINT=https://yourstorageaccount.blob.core.windows.net
export AZURE_STORAGE_CONTAINER=assets-container

# Database Configuration
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-postgres-server:5432/assets_manager
export SPRING_DATASOURCE_USERNAME=your-username
export SPRING_DATASOURCE_PASSWORD=your-password

# RabbitMQ Configuration
export SPRING_RABBITMQ_HOST=your-rabbitmq-host
export SPRING_RABBITMQ_USERNAME=your-username
export SPRING_RABBITMQ_PASSWORD=your-password
```

## Required Azure Permissions

The application requires the following permissions on the Azure Storage Account:
- `Storage Blob Data Reader` - to read blobs
- `Storage Blob Data Contributor` - to create, update, and delete blobs

## Migration from AWS S3

This application has been migrated from AWS S3 to Azure Blob Storage. Key changes:
- AWS S3 SDK replaced with Azure Storage Blob SDK
- AWS credentials replaced with Azure managed identity authentication
- S3 bucket configuration replaced with Azure Storage Account endpoint and container
- All functionality preserved: upload, download, list, delete operations
- Thumbnail generation workflow maintained