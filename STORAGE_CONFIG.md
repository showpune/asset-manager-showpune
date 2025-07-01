# Storage Configuration Guide

This document describes the storage configuration options available in the Asset Manager application.

## Storage Providers

The application supports three storage providers, activated using Spring profiles:

### 1. Azure Blob Storage (Default)
**Profile**: Default (when no specific storage profile is set)  
**When Active**: `!dev & !aws` (not dev profile and not aws profile)

**Configuration Properties:**
```properties
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

**Authentication**: Uses Azure Default Credential (managed identity, Azure CLI, environment variables, etc.)

**Example Startup:**
```bash
java -jar assets-manager-web.jar
```

### 2. AWS S3 Storage
**Profile**: `aws`  
**When Active**: When `aws` profile is explicitly activated

**Configuration Properties:**
```properties
aws.accessKey=your-access-key
aws.secretKey=your-secret-key
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
```

**Example Startup:**
```bash
java -jar assets-manager-web.jar --spring.profiles.active=aws
```

### 3. Local File Storage (Development)
**Profile**: `dev`  
**When Active**: When `dev` profile is explicitly activated

**Configuration Properties:**
```properties
local.storage.directory=../storage
```

**Example Startup:**
```bash
java -jar assets-manager-web.jar --spring.profiles.active=dev
```

## Profile Priority

The profile selection follows this priority:
1. If `dev` profile is active → Local File Storage
2. If `aws` profile is active → AWS S3 Storage  
3. Otherwise → Azure Blob Storage (default)

## Migration from S3 to Azure

To migrate from S3 to Azure:

1. **Set up Azure Storage Account** with a blob container
2. **Configure managed identity** for your application in Azure
3. **Update application properties** with Azure configuration
4. **Remove AWS profile** from startup command (use default Azure)
5. **Migrate existing data** from S3 to Azure Blob Storage

The application will automatically use Azure Blob Storage when the AWS profile is not active.