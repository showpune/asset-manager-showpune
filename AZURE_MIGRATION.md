# Azure Storage Migration Guide

This application has been successfully migrated from AWS S3 to Azure Storage Account.

## Configuration

### Azure Storage Account Setup

1. **Storage Account Endpoint**: Set your Azure Storage Account endpoint:
   ```properties
   azure.storage.account.endpoint=https://yourstorageaccount.blob.core.windows.net
   azure.storage.container.name=your-container-name
   ```

2. **Authentication**: The application uses Azure DefaultAzureCredential, which supports:
   - Azure CLI authentication (`az login`)
   - Managed Identity (when deployed to Azure)
   - Environment variables (AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID)
   - Visual Studio/VS Code authentication

### Profile Configuration

The application supports multiple storage providers through Spring profiles:

- **Azure Storage** (default): `spring.profiles.active=azure`
- **AWS S3** (legacy): `spring.profiles.active=s3`
- **Local Storage** (development): `spring.profiles.active=dev`

### Environment Variables

For production deployment, set these environment variables:

```bash
# Azure Storage Configuration
AZURE_STORAGE_ACCOUNT_ENDPOINT=https://yourstorageaccount.blob.core.windows.net
AZURE_STORAGE_CONTAINER_NAME=your-container-name

# Optional: For service principal authentication
AZURE_CLIENT_ID=your-client-id
AZURE_CLIENT_SECRET=your-client-secret
AZURE_TENANT_ID=your-tenant-id
```

## Features

- ✅ File upload to Azure Blob Storage
- ✅ File listing with metadata
- ✅ File viewing and download
- ✅ File deletion
- ✅ Thumbnail generation support (via RabbitMQ messaging)
- ✅ Database metadata storage

## API Endpoints

All endpoints have been updated to use `/storage` instead of `/s3`:

- `GET /storage` - List all files
- `GET /storage/upload` - Upload form
- `POST /storage/upload` - Upload file
- `GET /storage/view-page/{key}` - View file page
- `GET /storage/view/{key}` - Direct file access
- `POST /storage/delete/{key}` - Delete file

## Backward Compatibility

The application maintains backward compatibility:

- AWS S3 service is still available with `spring.profiles.active=s3`
- Database schema remains unchanged (S3Key and S3Url fields preserved)
- Original S3 configuration properties are still supported

## Development

For local development:

```bash
# Use local file storage
export SPRING_PROFILES_ACTIVE=dev

# Or use Azure Storage
export SPRING_PROFILES_ACTIVE=azure
export AZURE_STORAGE_ACCOUNT_ENDPOINT=https://yourstorageaccount.blob.core.windows.net
export AZURE_STORAGE_CONTAINER_NAME=your-container-name
```

## Build and Run

```bash
# Compile the application
./mvnw clean compile

# Run tests
./mvnw test

# Run the application
./mvnw spring-boot:run
```

## Migration Notes

1. **No breaking changes** to the public API - all URLs have been updated from `/s3` to `/storage`
2. **Database compatibility** - existing metadata records work without changes
3. **Profile-based switching** - easy to switch between storage providers
4. **Azure-first approach** - Azure Storage is now the default profile