# Asset Manager

Sample project for migration tool code remediation that manages assets in cloud storage.

## Overview

This application has been **migrated from AWS S3 to Azure Blob Storage** to demonstrate cloud-to-cloud migration patterns. The migration includes complete replacement of storage services, authentication mechanisms, and API integrations.

## Current Infrastructure

The project now uses the following Azure infrastructure:
* **Azure Blob Storage** for image storage, using managed identity authentication
* **RabbitMQ** for message queuing (to be migrated to Azure Service Bus in future iterations)
* **PostgreSQL** database for metadata storage (to be migrated to Azure Database for PostgreSQL in future iterations)

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

## Migration from AWS S3

This project demonstrates a complete migration from AWS S3 to Azure Blob Storage. Key changes include:

### Technical Changes
- **Dependencies**: Replaced AWS SDK with Azure Storage SDK
- **Authentication**: Changed from access keys to Azure DefaultAzureCredential
- **APIs**: Updated all storage operations to use Azure Blob Storage APIs
- **URLs**: Changed endpoints from `/s3/*` to `/blob/*`
- **Configuration**: Updated properties for Azure Storage Account and Container

### Migration Benefits
- **Enhanced Security**: Uses Azure Managed Identity instead of access keys
- **Better Integration**: Native integration with Azure ecosystem
- **Cost Optimization**: Azure Blob Storage access tiers for cost-effective storage
- **Improved Monitoring**: Azure Monitor and Storage Analytics

For detailed migration information, see [AZURE_MIGRATION_GUIDE.md](AZURE_MIGRATION_GUIDE.md).

## Configuration

### For Development (Local File System)
The application uses local file system storage when running with the `dev` profile:

```properties
spring.profiles.active=dev
local.storage.directory=../storage
```

### For Production (Azure Blob Storage)
Configure Azure Blob Storage for production deployment:

```properties
# Azure Storage Configuration
azure.storage.endpoint=https://yourstorageaccount.blob.core.windows.net
azure.storage.container=your-container-name
```

## Run Locally

**Prerequisites**: JDK 17+, Docker

Run the following commands to start the apps locally. This will:
* Use local file system instead of Azure Blob Storage to store images (dev profile)
* Launch RabbitMQ and PostgreSQL using Docker

Windows:
```batch
cd asset-manager-showpune
scripts\start.cmd
```

Linux:
```sh
cd asset-manager-showpune
scripts/start.sh
```

To stop, run `stop.cmd` or `stop.sh` in the `scripts` directory.

## Run with Azure Blob Storage

To test with actual Azure Blob Storage:

1. Set up Azure Storage Account and container
2. Configure authentication (Managed Identity, Service Principal, or connection string)
3. Update `application.properties` with Azure configuration
4. Remove or don't activate the `dev` profile
5. Run the application

See [AZURE_MIGRATION_GUIDE.md](AZURE_MIGRATION_GUIDE.md) for detailed setup instructions.

## Features

- **Image Upload**: Upload images to Azure Blob Storage
- **Image Management**: View, download, and delete images
- **Thumbnail Generation**: Automatic thumbnail creation via worker service
- **Responsive UI**: Modern web interface with drag-and-drop upload
- **Profile-based Configuration**: Easy switching between dev (local) and production (Azure) modes

## Technology Stack

- **Backend**: Spring Boot 3.4.3, Java 17
- **Storage**: Azure Blob Storage (production), Local File System (development)
- **Messaging**: RabbitMQ
- **Database**: PostgreSQL
- **Frontend**: Thymeleaf, Bootstrap 5
- **Build**: Maven
- **Authentication**: Azure DefaultAzureCredential