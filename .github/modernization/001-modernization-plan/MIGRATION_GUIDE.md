# Azure Migration Guide

## Overview
This guide documents the migration of the Asset Manager application from AWS infrastructure to Azure services.

## Migration Summary

### What Changed

| Component | Before (AWS) | After (Azure) | Authentication |
|-----------|-------------|---------------|----------------|
| Storage | AWS S3 | Azure Blob Storage | Managed Identity (DefaultAzureCredential) |
| Message Queue | RabbitMQ | Azure Service Bus | Managed Identity (DefaultAzureCredential) |
| Database | PostgreSQL | Azure Database for PostgreSQL | Managed Identity (recommended) |
| Java Version | 17 | 21 LTS | N/A |
| Spring Boot | 3.2.1 | 3.4.2 | N/A |

## Prerequisites

### Azure Resources Required

1. **Azure Storage Account**
   - Create a storage account
   - Create a container named `images` (or configure custom name)
   - Note the storage account name

2. **Azure Service Bus**
   - Create a Service Bus namespace (Standard tier or higher)
   - Create a queue named `image-processing`
   - Note the namespace name

3. **Azure Database for PostgreSQL** (Optional)
   - Create an Azure PostgreSQL Flexible Server
   - Create a database named `assets_manager`
   - Configure firewall rules

### Azure RBAC Roles Required

For the application to work with Managed Identity, assign the following RBAC roles to your application's managed identity:

1. **Storage Blob Data Contributor** - For Azure Blob Storage
   - Allows read, write, and delete access to blob containers and data

2. **Azure Service Bus Data Sender** - For Azure Service Bus
   - Allows sending messages to Service Bus queues and topics

3. **Azure Service Bus Data Receiver** - For Azure Service Bus
   - Allows receiving messages from Service Bus queues and topics

### Assigning RBAC Roles

#### Using Azure Portal:
1. Navigate to your Storage Account
2. Go to "Access Control (IAM)"
3. Click "Add role assignment"
4. Select "Storage Blob Data Contributor"
5. Assign to your application's managed identity

Repeat for Service Bus with appropriate roles.

#### Using Azure CLI:
```bash
# For Storage Account
az role assignment create \
  --role "Storage Blob Data Contributor" \
  --assignee <managed-identity-principal-id> \
  --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account-name>

# For Service Bus
az role assignment create \
  --role "Azure Service Bus Data Sender" \
  --assignee <managed-identity-principal-id> \
  --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.ServiceBus/namespaces/<servicebus-namespace>

az role assignment create \
  --role "Azure Service Bus Data Receiver" \
  --assignee <managed-identity-principal-id> \
  --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.ServiceBus/namespaces/<servicebus-namespace>
```

## Configuration

### Application Properties for Azure

Update the following properties in your `application-azure.properties` file:

**Web Module** (`web/src/main/resources/application-azure.properties`):
```properties
# Azure Blob Storage Configuration
azure.storage.account-name=<your-storage-account-name>
azure.storage.container-name=images

# Azure Service Bus Configuration
spring.cloud.azure.servicebus.namespace=<your-servicebus-namespace>
spring.cloud.azure.servicebus.pricing-tier=standard
spring.jms.servicebus.enabled=true
spring.jms.servicebus.idle-timeout=1800000
```

**Worker Module** (`worker/src/main/resources/application-azure.properties`):
```properties
# Azure Blob Storage Configuration
azure.storage.account-name=<your-storage-account-name>
azure.storage.container-name=images

# Azure Service Bus Configuration
spring.cloud.azure.servicebus.namespace=<your-servicebus-namespace>
spring.cloud.azure.servicebus.pricing-tier=standard
spring.jms.servicebus.enabled=true
spring.jms.servicebus.idle-timeout=1800000
```

### Environment Variables

When running the application with Azure profile, set:
```bash
export SPRING_PROFILES_ACTIVE=azure
```

## Running the Application

### Local Development with Azure Services

1. **Authenticate with Azure**
   ```bash
   az login
   ```

2. **Set the active profile**
   ```bash
   export SPRING_PROFILES_ACTIVE=azure
   ```

3. **Start the Web Module**
   ```bash
   cd web
   mvn spring-boot:run -Dspring-boot.run.profiles=azure
   ```

4. **Start the Worker Module**
   ```bash
   cd worker
   mvn spring-boot:run -Dspring-boot.run.profiles=azure
   ```

### Deploying to Azure

When deploying to Azure App Service or Azure Container Apps:

1. **Enable Managed Identity**
   - Enable System-assigned managed identity for your Azure resource
   - Assign the required RBAC roles (see above)

2. **Configure Application Settings**
   - Set `SPRING_PROFILES_ACTIVE=azure`
   - Set `AZURE_STORAGE_ACCOUNT_NAME`
   - Set `SPRING_CLOUD_AZURE_SERVICEBUS_NAMESPACE`
   - Configure database connection settings

3. **Deploy the Application**
   - Use Azure CLI, Azure DevOps, or GitHub Actions
   - The application will automatically use the managed identity for authentication

## Architecture Changes

### Storage Layer

- **Before**: AWS S3 with access key/secret key authentication
- **After**: Azure Blob Storage with managed identity authentication

The `AzureBlobStorageService` class implements the same `StorageService` interface as the AWS S3 service, ensuring compatibility with existing code.

Key changes:
- Uses `BlobServiceClient` instead of `S3Client`
- Uses `DefaultAzureCredential` for authentication (no keys needed)
- Automatically creates containers if they don't exist
- Maintains same API surface for upload, download, list, and delete operations

### Message Queue

- **Before**: RabbitMQ with username/password authentication
- **After**: Azure Service Bus with managed identity authentication using JMS API

The application uses the Spring Cloud Azure Service Bus JMS starter, which provides a JMS-compatible interface for Azure Service Bus. This minimizes code changes in the worker module.

Key changes:
- Uses JMS API over Azure Service Bus
- Maintains same message format and processing logic
- Uses `DefaultAzureCredential` for authentication
- Supports manual acknowledgment mode for reliable message processing

### Profile-Based Configuration

The application uses Spring profiles to switch between different infrastructures:

- **`dev` profile**: Uses local file storage and RabbitMQ
- **`azure` profile**: Uses Azure Blob Storage and Azure Service Bus
- **Default (no profile)**: Uses AWS S3 and RabbitMQ

Profile selection is controlled by the `@Profile` annotation on configuration and service beans.

## Managed Identity Authentication

### What is DefaultAzureCredential?

`DefaultAzureCredential` is a credential chain that tries multiple authentication methods in order:
1. Environment variables
2. Managed Identity
3. Azure CLI
4. IntelliJ/Visual Studio Code
5. Azure PowerShell

This makes it easy to:
- Develop locally using Azure CLI authentication
- Deploy to Azure using managed identity (no credentials in code)
- Run in CI/CD using service principal environment variables

### Benefits

1. **Security**: No credentials stored in code or configuration files
2. **Simplicity**: Same code works in development and production
3. **Best Practice**: Follows Azure security best practices
4. **Zero Configuration**: Works automatically when deployed to Azure with managed identity

## Testing

### Unit Tests

The existing unit tests continue to work without modification as they use mocking frameworks.

### Integration Tests

For integration testing with Azure services:

1. **Local Testing**: Use Azure CLI authentication
   ```bash
   az login
   mvn test -Dspring.profiles.active=azure
   ```

2. **CI/CD Testing**: Use service principal
   ```bash
   export AZURE_CLIENT_ID=<client-id>
   export AZURE_CLIENT_SECRET=<client-secret>
   export AZURE_TENANT_ID=<tenant-id>
   mvn test -Dspring.profiles.active=azure
   ```

## Troubleshooting

### Authentication Issues

**Problem**: `ManagedIdentityCredential authentication failed`

**Solutions**:
- Ensure managed identity is enabled on your Azure resource
- Verify RBAC roles are assigned correctly
- Check that the role assignments have propagated (can take a few minutes)
- For local development, ensure you're logged in: `az login`

### Connection Issues

**Problem**: Cannot connect to Azure Storage/Service Bus

**Solutions**:
- Verify network connectivity
- Check firewall rules on Azure resources
- Ensure correct resource names in configuration
- Verify the Azure resources exist in the correct subscription

### Queue/Blob Operations Fail

**Problem**: Operations fail with permission errors

**Solutions**:
- Verify RBAC roles are correctly assigned
- Check that you're using the right role (Sender vs Receiver for Service Bus)
- Ensure the queue/container exists
- Check the resource name in configuration

## Rollback Plan

If issues occur after migration:

1. **Switch back to AWS profile**: Remove `azure` from active profiles or set `SPRING_PROFILES_ACTIVE` to empty
2. **Revert configuration**: Update application properties to use AWS credentials
3. **Redeploy**: Deploy the application without the Azure profile

The application maintains backward compatibility with AWS services.

## Performance Considerations

### Azure Blob Storage
- Comparable performance to S3 for most operations
- Consider using Azure CDN for improved content delivery
- Blob Storage offers different access tiers (Hot, Cool, Archive) for cost optimization

### Azure Service Bus
- Standard tier supports up to 256 KB message size
- Premium tier recommended for high-throughput scenarios
- Supports partitioning for scalability
- Message TTL and dead-letter queues for reliability

## Cost Optimization

1. **Storage Tiers**: Use Cool or Archive tiers for infrequently accessed images
2. **Service Bus Tiers**: Start with Standard tier, upgrade to Premium only if needed
3. **Lifecycle Management**: Configure blob lifecycle management policies
4. **Reserved Capacity**: Consider reserved capacity for predictable workloads

## Security Best Practices

1. **Managed Identity**: Always use managed identity in production
2. **Network Security**: Use Private Endpoints for Storage and Service Bus
3. **Data Encryption**: Enable encryption at rest (default) and in transit (TLS)
4. **RBAC**: Follow principle of least privilege when assigning roles
5. **Monitoring**: Enable Azure Monitor and Application Insights
6. **Key Rotation**: No need with managed identity, but rotate service principal secrets if used

## Monitoring and Logging

### Application Insights Integration

Add Application Insights to monitor the application:

```xml
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter-monitor</artifactId>
</dependency>
```

Configure in `application-azure.properties`:
```properties
spring.cloud.azure.monitor.enabled=true
spring.cloud.azure.monitor.connection-string=${APPLICATIONINSIGHTS_CONNECTION_STRING}
```

### Metrics to Monitor

- Blob Storage: Request count, latency, throttling
- Service Bus: Message count, dead letters, processing time
- Application: Request rate, error rate, response time

## Support and Resources

### Documentation
- [Azure Blob Storage Java SDK](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java)
- [Azure Service Bus JMS](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-java-how-to-use-jms-api-amqp)
- [Spring Cloud Azure](https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/)
- [DefaultAzureCredential](https://learn.microsoft.com/en-us/azure/developer/java/sdk/identity-azure-hosted-auth)

### Azure Support
- Open a support ticket in Azure Portal
- Use Azure Community Support forums
- Contact Microsoft Support for production issues
