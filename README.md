# Asset Manager
Sample project for migration tool code remediation that manages assets in cloud storage.

## Current Infrastructure
The project currently uses the following infrastructure:
* AWS S3 for image storage, using password-based authentication (access key/secret key)
* RabbitMQ for message queuing, using password-based authentication
* PostgreSQL database for metadata storage, using password-based authentication

## Current Architecture
```mermaid
flowchart TD

%% Applications
WebApp[Web Application]
Worker[Worker Service]

%% Storage Components
S3[(AWS S3)]
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
WebApp -->|Store Original Image| S3
WebApp -->|Store Original Image| LocalFS
WebApp -->|Send Processing Message| RabbitMQ
WebApp -->|Store Metadata| PostgreSQL
WebApp -->|Retrieve Images| S3
WebApp -->|Retrieve Images| LocalFS
WebApp -->|Retrieve Metadata| PostgreSQL

%% RabbitMQ Flow
RabbitMQ -->|Push Message| Worker

%% Worker Flow
Worker -->|Download Original| S3
Worker -->|Download Original| LocalFS
Worker -->|Upload Thumbnail| S3
Worker -->|Upload Thumbnail| LocalFS
Worker -->|Store Metadata| PostgreSQL
Worker -->|Retrieve Metadata| PostgreSQL

%% Styling
classDef app fill:#90caf9,stroke:#0d47a1,color:#0d47a1
classDef storage fill:#a5d6a7,stroke:#1b5e20,color:#1b5e20
classDef broker fill:#ffcc80,stroke:#e65100,color:#e65100
classDef db fill:#ce93d8,stroke:#4a148c,color:#4a148c
classDef queue fill:#fff59d,stroke:#f57f17,color:#f57f17
classDef user fill:#ef9a9a,stroke:#b71c1c,color:#b71c1c

class WebApp,Worker app
class S3,LocalFS storage
class RabbitMQ broker
class PostgreSQL db
class Queue,RetryQueue queue
class User user
```
Password-based authentication

## Migrated Infrastructure
After migration, the project will use the following Azure services:
* Azure Blob Storage for image storage, using managed identity authentication
* Azure Service Bus for message queuing, using managed identity authentication
* Azure Database for PostgreSQL for metadata storage, using managed identity authentication

## Migrated Architecture
```mermaid
flowchart TD

%% Applications
WebApp[Web Application]
Worker[Worker Service]

%% Azure Storage Components
AzBlob[(Azure Blob Storage)]
LocalFS[("Local File System<br/>dev only")]

%% Azure Message Broker
ServiceBus(Azure Service Bus)

%% Azure Database
AzPostgreSQL[(Azure PostgreSQL)]

%% User
User([User])

%% User Flow
User -->|Upload Image| WebApp
User -->|View Images| WebApp

%% Web App Flows
WebApp -->|Store Original Image| AzBlob
WebApp -->|Store Original Image| LocalFS
WebApp -->|Send Processing Message| ServiceBus
WebApp -->|Store Metadata| AzPostgreSQL
WebApp -->|Retrieve Images| AzBlob
WebApp -->|Retrieve Images| LocalFS
WebApp -->|Retrieve Metadata| AzPostgreSQL

%% Service Bus Flow
ServiceBus -->|Push Message| Worker

%% Worker Flow
Worker -->|Download Original| AzBlob
Worker -->|Download Original| LocalFS
Worker -->|Upload Thumbnail| AzBlob
Worker -->|Upload Thumbnail| LocalFS
Worker -->|Store Metadata| AzPostgreSQL
Worker -->|Retrieve Metadata| AzPostgreSQL

%% Styling
classDef app fill:#90caf9,stroke:#0d47a1,color:#0d47a1
classDef storage fill:#68B3A1,stroke:#006064,color:#006064
classDef broker fill:#B39DDB,stroke:#4527A0,color:#4527A0
classDef db fill:#90CAF9,stroke:#1565C0,color:#1565C0
classDef queue fill:#fff59d,stroke:#f57f17,color:#f57f17
classDef user fill:#ef9a9a,stroke:#b71c1c,color:#b71c1c

class WebApp,Worker app
class AzBlob,LocalFS storage
class ServiceBus broker
class AzPostgreSQL db
class Queue,RetryQueue queue
class User user
```
Managed identity based authentication

## Run Locally

**Prerequisites**: JDK, Docker

### Running with Local File System and RabbitMQ (Development)

Run the following commands to start the apps locally. This will:
* Use local file system instead of cloud storage to store the image
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

### Running with AWS S3 and RabbitMQ

To use AWS S3 instead of local file system:

1. Set the spring profile to `s3` (not `dev`) in both web and worker applications
2. Configure your AWS credentials in the application-s3.properties files
3. Run the applications

### Running with Azure Storage Account and Service Bus

To use Azure Blob Storage and Azure Service Bus:

1. Set the spring profile to `azure` in both web and worker applications
2. Configure your Azure credentials using Azure Managed Identity
3. Set the following environment variables:
   - `AZURE_CLIENT_ID`: Your Azure managed identity client ID
   - `SERVICE_BUS_NAMESPACE`: Your Azure Service Bus namespace
4. Update the Azure configuration in application-azure.properties files
5. Run the applications

To stop, run `stop.cmd` or `stop.sh` in the `scripts` directory.

## Migration Guide

### Easy Configuration Switching

Use the provided configuration scripts to easily switch between storage providers:

**Linux/Mac:**
```bash
./configure.sh --azure    # Switch to Azure configuration
./configure.sh --s3       # Switch to AWS S3 configuration  
./configure.sh --dev      # Switch to development configuration
./configure.sh --status   # Show current configuration
```

**Windows:**
```cmd
configure.cmd --azure     # Switch to Azure configuration
configure.cmd --s3        # Switch to AWS S3 configuration
configure.cmd --dev       # Switch to development configuration
configure.cmd --status    # Show current configuration
```

### Migrating from AWS S3 to Azure Storage Account

This project supports both AWS S3 and Azure Blob Storage. To migrate from S3 to Azure:

1. **Set up Azure Resources**:
   - Create an Azure Storage Account
   - Create a blob container for image storage
   - Create an Azure Service Bus namespace and queue
   - Set up Azure Managed Identity for authentication

2. **Configure Environment Variables**:
   ```bash
   export AZURE_CLIENT_ID=your-managed-identity-client-id
   export SERVICE_BUS_NAMESPACE=your-servicebus-namespace.servicebus.windows.net
   ```

3. **Update Configuration**:
   - Modify `application-azure.properties` files with your Azure resource details
   - Set the storage account endpoint and container name

4. **Switch Application Profile**:
   - Change from `s3` profile to `azure` profile
   - Or start applications with `-Dspring.profiles.active=azure`

5. **Data Migration** (if needed):
   - Use Azure Storage Migration tools to copy existing data from S3 to Azure Blob Storage
   - Update any database records that reference S3 URLs to point to Azure Blob Storage URLs

The application will automatically use Azure Blob Storage for new uploads and Azure Service Bus for message processing when running with the `azure` profile.