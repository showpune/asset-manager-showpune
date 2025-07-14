# Asset Manager
Sample project for migration tool code remediation that manages assets in cloud storage.

## Current Infrastructure (Azure Storage Account)
The project has been migrated to use Azure services:
* **Azure Blob Storage** for image storage, using DefaultAzureCredential authentication
* **RabbitMQ** for message queuing (can be migrated to Azure Service Bus later)
* **PostgreSQL** database for metadata storage (can be migrated to Azure Database for PostgreSQL later)

## Architecture
```mermaid
flowchart TD

%% Applications
WebApp[Web Application]
Worker[Worker Service]

%% Azure Storage Components
AzBlob[(Azure Blob Storage)]
LocalFS[("Local File System<br/>dev only")]

%% Message Broker
RabbitMQ(RabbitMQ)

%% Database
PostgreSQL[(PostgreSQL)]

%% User
User([User])

%% User Flow
User -->|Upload Image| WebApp
User -->|View Images| WebApp

%% Web App Flows
WebApp -->|Store Original Image| AzBlob
WebApp -->|Store Original Image| LocalFS
WebApp -->|Send Processing Message| RabbitMQ
WebApp -->|Store Metadata| PostgreSQL
WebApp -->|Retrieve Images| AzBlob
WebApp -->|Retrieve Images| LocalFS
WebApp -->|Retrieve Metadata| PostgreSQL

%% RabbitMQ Flow
RabbitMQ -->|Push Message| Worker

%% Worker Flow
Worker -->|Download Original| AzBlob
Worker -->|Download Original| LocalFS
Worker -->|Upload Thumbnail| AzBlob
Worker -->|Upload Thumbnail| LocalFS
Worker -->|Store Metadata| PostgreSQL
Worker -->|Retrieve Metadata| PostgreSQL

%% Styling
classDef app fill:#90caf9,stroke:#0d47a1,color:#0d47a1
classDef storage fill:#68B3A1,stroke:#006064,color:#006064
classDef broker fill:#ffcc80,stroke:#e65100,color:#e65100
classDef db fill:#ce93d8,stroke:#4a148c,color:#4a148c
classDef queue fill:#fff59d,stroke:#f57f17,color:#f57f17
classDef user fill:#ef9a9a,stroke:#b71c1c,color:#b71c1c

class WebApp,Worker app
class AzBlob,LocalFS storage
class RabbitMQ broker
class PostgreSQL db
class Queue,RetryQueue queue
class User user
```

## Configuration

### Azure Storage Account Setup

1. **Create Azure Storage Account**:
   ```bash
   # Using Azure CLI
   az storage account create \
     --name yourstorageaccount \
     --resource-group your-resource-group \
     --location eastus \
     --sku Standard_LRS
   ```

2. **Create Container**:
   ```bash
   az storage container create \
     --name your-container-name \
     --account-name yourstorageaccount
   ```

3. **Set up Authentication**:
   
   **Option A: Using Azure CLI (for local development)**
   ```bash
   az login
   ```
   
   **Option B: Using Managed Identity (for Azure hosting)**
   - Enable System Assigned Managed Identity on your Azure App Service
   - Grant the Managed Identity "Storage Blob Data Contributor" role on the storage account
   
   **Option C: Using Service Principal**
   ```bash
   # Create service principal
   az ad sp create-for-rbac --name "asset-manager-sp"
   
   # Assign role to storage account
   az role assignment create \
     --assignee <service-principal-id> \
     --role "Storage Blob Data Contributor" \
     --scope /subscriptions/<subscription-id>/resourceGroups/<resource-group>/providers/Microsoft.Storage/storageAccounts/<storage-account>
   ```

4. **Update Configuration**:

   **For Web Module** (`web/src/main/resources/application.properties`):
   ```properties
   # Azure Storage Configuration
   azure.storage.account-name=yourstorageaccount
   azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
   azure.storage.container-name=your-container-name
   ```

   **For Worker Module** (`worker/src/main/resources/application.properties`):
   ```properties
   # Azure Storage Configuration
   azure.storage.account-name=yourstorageaccount
   azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
   azure.storage.container-name=your-container-name
   ```

5. **Environment Variables (Optional)**:
   If using Service Principal authentication, set these environment variables:
   ```bash
   export AZURE_CLIENT_ID=<service-principal-id>
   export AZURE_CLIENT_SECRET=<service-principal-secret>
   export AZURE_TENANT_ID=<tenant-id>
   ```

### Database Migration (Required)

Since the field names have changed from S3-specific to Azure-specific, you'll need to update your database schema:

```sql
-- Update existing tables to use new field names
ALTER TABLE image_metadata RENAME COLUMN s3_key TO blob_key;
ALTER TABLE image_metadata RENAME COLUMN s3_url TO blob_url;
```

Or for a fresh start:
```sql
-- Drop existing tables and let JPA recreate them with new schema
DROP TABLE image_metadata;
-- Application will recreate the table with new field names on restart
```

## Run Locally

**Prerequisites**: JDK, Docker

Run the following commands to start the apps locally. This will:
* Use local file system instead of S3 to store the image
* Launch RabbitMQ and PostgreSQL using Docker

Windows:

```batch
cd asset-manager
scripts\start.cmd
```

Linux:

```sh
cd asset-manager
scripts/start.sh
```

To stop, run `stop.cmd` or `stop.sh` in the `scripts` directory.