# Azure Storage Troubleshooting Guide

This guide covers common issues you might encounter when running the Asset Manager with Azure Storage Account.

## Common Issues and Solutions

### 1. Authentication Issues

#### Problem: `DefaultAzureCredentialError: Unable to acquire token`

**Symptoms:**
```
com.azure.identity.CredentialUnavailableException: DefaultAzureCredential authentication unavailable. Multiple attempts failed...
```

**Solutions:**

1. **For Local Development:**
   ```bash
   # Login to Azure CLI
   az login
   
   # Verify you're logged in
   az account show
   ```

2. **For Azure App Service:**
   - Enable System Assigned Managed Identity in the Azure portal
   - Grant Storage Blob Data Contributor role to the Managed Identity

3. **Using Service Principal:**
   ```bash
   # Set environment variables
   export AZURE_CLIENT_ID=your-client-id
   export AZURE_CLIENT_SECRET=your-client-secret
   export AZURE_TENANT_ID=your-tenant-id
   ```

#### Problem: `Access Denied` or `403 Forbidden`

**Symptoms:**
```
Status code 403, "This request is not authorized to perform this operation"
```

**Solutions:**

1. **Check Role Assignment:**
   ```bash
   # List role assignments for your identity
   az role assignment list --assignee <your-identity-id>
   
   # Assign the correct role
   az role assignment create \
     --assignee <your-identity-id> \
     --role "Storage Blob Data Contributor" \
     --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account>
   ```

2. **Verify Container Exists:**
   ```bash
   # List containers
   az storage container list --account-name your-storage-account
   
   # Create container if missing
   az storage container create \
     --name your-container-name \
     --account-name your-storage-account
   ```

### 2. Configuration Issues

#### Problem: `Container not found`

**Symptoms:**
```
The specified container does not exist.
```

**Solutions:**

1. **Verify Configuration:**
   ```properties
   # Check these properties in application.properties
   azure.storage.account-name=your-storage-account-name
   azure.storage.endpoint=https://your-storage-account-name.blob.core.windows.net
   azure.storage.container-name=your-container-name
   ```

2. **Create Missing Container:**
   ```bash
   az storage container create \
     --name your-container-name \
     --account-name your-storage-account
   ```

#### Problem: `Storage account not found`

**Symptoms:**
```
The specified account does not exist.
```

**Solutions:**

1. **Verify Storage Account Name:**
   ```bash
   # List storage accounts
   az storage account list --query "[].name"
   ```

2. **Check Endpoint URL:**
   - Ensure the endpoint matches: `https://{account-name}.blob.core.windows.net`
   - Verify the account name in configuration

### 3. Database Migration Issues

#### Problem: `Column 's3_key' doesn't exist`

**Symptoms:**
```
org.postgresql.util.PSQLException: ERROR: column "s3_key" does not exist
```

**Solutions:**

1. **Manual Schema Update:**
   ```sql
   -- Update existing table
   ALTER TABLE image_metadata RENAME COLUMN s3_key TO blob_key;
   ALTER TABLE image_metadata RENAME COLUMN s3_url TO blob_url;
   ```

2. **Fresh Start (Development Only):**
   ```sql
   -- Drop and recreate table
   DROP TABLE image_metadata;
   -- Restart application to recreate with new schema
   ```

3. **Flyway Migration (Recommended for Production):**
   Create migration file `V2__migrate_to_azure.sql`:
   ```sql
   ALTER TABLE image_metadata RENAME COLUMN s3_key TO blob_key;
   ALTER TABLE image_metadata RENAME COLUMN s3_url TO blob_url;
   ```

### 4. Network and Connectivity Issues

#### Problem: `Connection timeout` or `Network unreachable`

**Symptoms:**
```
java.net.SocketTimeoutException: Read timed out
```

**Solutions:**

1. **Check Network Connectivity:**
   ```bash
   # Test connectivity to Azure Storage
   nslookup your-storage-account.blob.core.windows.net
   curl -I https://your-storage-account.blob.core.windows.net
   ```

2. **Firewall/Proxy Configuration:**
   - Ensure outbound HTTPS (port 443) is allowed
   - Configure proxy settings if needed:
   ```java
   System.setProperty("https.proxyHost", "proxy.example.com");
   System.setProperty("https.proxyPort", "8080");
   ```

3. **Azure Storage Firewall:**
   - Check if storage account has network restrictions
   - Add your IP/network to allowed list in Azure portal

### 5. Performance Issues

#### Problem: Slow upload/download operations

**Solutions:**

1. **Check Storage Tier:**
   - Use Hot tier for frequently accessed data
   - Cool/Archive tiers have slower access times

2. **Enable CDN:**
   ```bash
   # Create CDN profile and endpoint
   az cdn profile create --name mycdnprofile --resource-group myresourcegroup
   az cdn endpoint create --name myendpoint --profile-name mycdnprofile --resource-group myresourcegroup --origin your-storage-account.blob.core.windows.net
   ```

3. **Optimize File Sizes:**
   - Compress images before upload
   - Use appropriate thumbnail sizes

### 6. Development Environment Issues

#### Problem: Different behavior between local and Azure

**Solutions:**

1. **Use Application Profiles:**
   ```properties
   # application-local.properties
   spring.profiles.active=local
   azure.storage.account-name=localdevaccount
   
   # application-production.properties
   spring.profiles.active=production
   azure.storage.account-name=prodaccount
   ```

2. **Environment-Specific Configuration:**
   ```java
   @Profile("!dev")
   @Service
   public class AzureBlobService implements StorageService {
       // Azure implementation
   }
   
   @Profile("dev")
   @Service
   public class LocalFileStorageService implements StorageService {
       // Local file system implementation
   }
   ```

## Debugging Tips

### 1. Enable Debug Logging

Add to `application.properties`:
```properties
# Azure SDK logging
logging.level.com.azure=DEBUG
logging.level.com.azure.storage=DEBUG
logging.level.com.azure.identity=DEBUG

# Application logging
logging.level.com.microsoft.migration.assets=DEBUG
```

### 2. Test Azure Connection

Create a simple test endpoint:
```java
@RestController
public class AzureTestController {
    
    @Autowired
    private BlobServiceClient blobServiceClient;
    
    @GetMapping("/test/azure")
    public ResponseEntity<String> testAzureConnection() {
        try {
            var properties = blobServiceClient.getProperties();
            return ResponseEntity.ok("Azure Storage connected successfully. Account: " + properties.getDefaultServiceVersion());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Azure Storage connection failed: " + e.getMessage());
        }
    }
}
```

### 3. Monitor Storage Metrics

Use Azure CLI to check storage metrics:
```bash
# Check storage account metrics
az monitor metrics list \
  --resource "/subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account>" \
  --metric "Transactions" \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z
```

## Health Check Implementation

Add comprehensive health checks:

```java
@Component
public class AzureStorageHealthIndicator implements HealthIndicator {
    
    @Autowired
    private BlobServiceClient blobServiceClient;
    
    @Value("${azure.storage.container-name}")
    private String containerName;
    
    @Override
    public Health health() {
        try {
            // Test service connection
            var serviceProperties = blobServiceClient.getProperties();
            
            // Test container access
            var containerClient = blobServiceClient.getBlobContainerClient(containerName);
            var containerExists = containerClient.exists();
            
            if (!containerExists) {
                return Health.down()
                    .withDetail("container", "Container '" + containerName + "' does not exist")
                    .build();
            }
            
            return Health.up()
                .withDetail("storage", "Azure Storage accessible")
                .withDetail("container", containerName + " exists")
                .withDetail("serviceVersion", serviceProperties.getDefaultServiceVersion())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("storage", "Azure Storage not accessible")
                .withException(e)
                .build();
        }
    }
}
```

## Emergency Procedures

### 1. Quick Rollback to Local Storage

If Azure Storage is completely inaccessible, activate the local storage profile:

```properties
# Add to application.properties temporarily
spring.profiles.active=dev
```

This will switch to local file storage until Azure issues are resolved.

### 2. Data Recovery

If data seems lost, check:

1. **Different Container:**
   ```bash
   # List all containers
   az storage container list --account-name your-storage-account
   ```

2. **Soft Delete (if enabled):**
   ```bash
   # List deleted blobs
   az storage blob list --container-name your-container --account-name your-storage-account --include-deleted
   ```

3. **Different Storage Account:**
   ```bash
   # Search across all storage accounts
   az storage account list --query "[?contains(name, 'asset')]"
   ```

### 3. Performance Emergency

If experiencing severe performance issues:

1. **Switch to different region:**
   ```properties
   azure.storage.endpoint=https://your-storage-account-west.blob.core.windows.net
   ```

2. **Enable CDN immediately:**
   ```properties
   # Use CDN endpoint temporarily
   azure.storage.endpoint=https://your-cdn-endpoint.azureedge.net
   ```

## Contact Information

For additional support:
- Azure Support: Create support ticket in Azure portal
- Application Team: Check internal documentation for team contacts
- Emergency Escalation: Follow your organization's emergency procedures

## Useful Azure CLI Commands

```bash
# Check authentication
az account show

# List storage accounts
az storage account list

# Check container permissions
az storage container show-permission --name your-container --account-name your-storage-account

# Download blob for inspection
az storage blob download --container-name your-container --name blob-name --file local-file --account-name your-storage-account

# Check storage account keys (if using key-based auth)
az storage account keys list --account-name your-storage-account

# Monitor storage logs
az storage logging show --account-name your-storage-account
```