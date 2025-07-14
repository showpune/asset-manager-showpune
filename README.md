# Asset Manager
Sample project for migration tool code remediation that manages assets in cloud storage.

## Current Infrastructure (After Migration)
The project now uses Azure infrastructure instead of AWS:
* **Azure Blob Storage** for image storage, using DefaultAzureCredential authentication
* **RabbitMQ** for message queuing (unchanged - could be migrated to Azure Service Bus in future)
* **PostgreSQL** database for metadata storage (unchanged - could be migrated to Azure Database for PostgreSQL in future)

## Migration Summary

This project has been **successfully migrated from AWS S3 to Azure Blob Storage**.

### What Changed:
- **Dependencies**: AWS SDK replaced with Azure Storage Blob SDK (12.29.0) and Azure Identity SDK (1.15.4)
- **Configuration**: AWS credentials replaced with Azure storage account endpoint and container configuration
- **Authentication**: Changed from AWS access keys to Azure DefaultAzureCredential (supports managed identity)
- **Code**: All S3-specific operations replaced with Azure Blob Storage equivalents
- **UI**: Updated from "AWS S3 Asset Manager" to "Azure Storage Asset Manager"
- **URLs**: Changed from `/s3/*` to `/storage/*` for storage-agnostic naming

### What Remained Unchanged:
- Application architecture and flow
- Database models and operations
- RabbitMQ messaging
- Worker thumbnail processing logic
- Web UI functionality

## Configuration

### Azure Storage Configuration
Update the following properties in both `web/src/main/resources/application.properties` and `worker/src/main/resources/application.properties`:

```properties
# Azure Storage Configuration
azure.storage.account-name=your-storage-account-name
azure.storage.endpoint=https://your-storage-account-name.blob.core.windows.net
azure.storage.container-name=your-container-name
```

### Authentication
The application uses `DefaultAzureCredential` which supports multiple authentication methods in order:
1. Environment variables (`AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`)
2. Managed Identity (when running in Azure)
3. Azure CLI (when running locally with `az login`)
4. Visual Studio/VS Code (when signed in)

For local development, the easiest option is to use Azure CLI:
```bash
az login
```

## Current Architecture
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

## Run Locally

**Prerequisites**: JDK 17+, Docker

For local development, the application uses the `dev` profile which automatically uses local file system storage instead of Azure Blob Storage.

Run the following commands to start the apps locally. This will:
* Use local file system instead of Azure Blob Storage to store images
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

## Azure Deployment

When deploying to Azure:

1. **Create Azure Storage Account**: 
   - Create a storage account in Azure
   - Create a container for storing images
   - Update configuration with storage account name and container name

2. **Configure Authentication**:
   - Enable managed identity on your Azure service (App Service, Container Instances, etc.)
   - Grant the managed identity "Storage Blob Data Contributor" role on the storage account

3. **Update Configuration**:
   - Set the Azure storage configuration properties
   - Ensure `dev` profile is not active in production

## Migration Notes

- The database models still reference `s3Key` and `s3Url` fields for backward compatibility. In a future update, these could be renamed to `storageKey` and `storageUrl`.
- The application is now storage-agnostic and could be extended to support other storage providers (Google Cloud Storage, etc.) by implementing the `StorageService` interface.
- RabbitMQ and PostgreSQL could be further migrated to Azure Service Bus and Azure Database for PostgreSQL for a fully Azure-native solution.