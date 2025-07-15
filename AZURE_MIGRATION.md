# Azure Storage Migration Guide

This application has been migrated to support both AWS S3 and Azure Storage Account as storage backends.

## Storage Options

### 1. Azure Storage Account (Recommended)
- Modern cloud storage solution
- Better integration with Azure services
- Uses Azure SDK for Java

### 2. AWS S3 (Legacy Support)
- Original storage backend
- Maintained for backward compatibility

### 3. Local File Storage (Development)
- For local development only
- Uses local file system

## Configuration

### Using Azure Storage Account

Set the active profile to `azure`:
```
--spring.profiles.active=azure
```

Configure the following properties:
```properties
azure.storage.account.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container.name=your-container-name
```

### Using AWS S3

Set the active profile to `s3`:
```
--spring.profiles.active=s3
```

Configure the following properties:
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

### Using Local Storage (Development)

Set the active profile to `dev`:
```
--spring.profiles.active=dev
```

## Running the Application

### Web Module
```bash
# With Azure Storage
java -jar web/target/assets-manager-web-*.jar --spring.profiles.active=azure

# With AWS S3
java -jar web/target/assets-manager-web-*.jar --spring.profiles.active=s3

# With Local Storage (Development)
java -jar web/target/assets-manager-web-*.jar --spring.profiles.active=dev
```

### Worker Module
```bash
# With Azure Storage
java -jar worker/target/assets-manager-worker-*.jar --spring.profiles.active=azure

# With AWS S3
java -jar worker/target/assets-manager-worker-*.jar --spring.profiles.active=s3

# With Local Storage (Development)
java -jar worker/target/assets-manager-worker-*.jar --spring.profiles.active=dev
```

## Authentication

### Azure Storage Account
The application uses DefaultAzureCredential which supports multiple authentication methods:
1. Environment variables (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID)
2. Managed Identity (when running in Azure)
3. Azure CLI (for development)
4. IntelliJ IDEA Azure plugin
5. Visual Studio Code Azure plugin

### AWS S3
Uses access key and secret key configured in the properties file.

## Database Schema

The ImageMetadata entity supports both storage backends:
- S3: `s3Key`, `s3Url`, `thumbnailKey`, `thumbnailUrl`
- Azure: `blobKey`, `blobUrl`, `thumbnailBlobKey`, `thumbnailBlobUrl`

This allows for seamless migration between storage backends without data loss.