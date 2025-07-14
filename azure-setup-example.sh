# Example configuration for Azure Storage Account setup
# This file shows how to configure the application to use Azure Storage Account

# Step 1: Create Azure Storage Account
# az storage account create --name yourstorageaccount --resource-group yourresourcegroup --location eastus --sku Standard_LRS

# Step 2: Create container
# az storage container create --name assets --account-name yourstorageaccount

# Step 3: Setup authentication
# Option A: Service Principal (recommended for production)
export AZURE_CLIENT_ID="your-service-principal-client-id"
export AZURE_CLIENT_SECRET="your-service-principal-client-secret"  
export AZURE_TENANT_ID="your-azure-tenant-id"

# Option B: Use Azure CLI (for development)
# az login

# Step 4: Activate Azure profile
export SPRING_PROFILES_ACTIVE="azure"

# Step 5: Set Azure Storage configuration
export AZURE_STORAGE_ENDPOINT="https://yourstorageaccount.blob.core.windows.net"
export AZURE_STORAGE_CONTAINER="assets"

# Step 6: Start the application
# java -jar web/target/assets-manager-web-0.0.1-SNAPSHOT.jar
# java -jar worker/target/assets-manager-worker-0.0.1-SNAPSHOT.jar