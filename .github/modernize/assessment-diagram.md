# Architecture Diagram - Assets Manager Application

## Overview

This document contains architecture diagrams for the Assets Manager application, a Spring Boot-based system for managing file uploads, storage, and thumbnail generation.

## Application Context

```mermaid
C4Context
    title System Context - Assets Manager Application

    Person(user, "End User", "Uploads and manages asset files")
    
    System(assetManager, "Assets Manager", "Multi-module Spring Boot application for asset management")
    
    System_Ext(s3, "AWS S3", "Object storage for files and thumbnails")
    System_Ext(rabbitmq, "RabbitMQ", "Message broker for async processing")
    System_Ext(postgres, "PostgreSQL", "Relational database for metadata")
    
    Rel(user, assetManager, "Uploads files, views assets")
    Rel(assetManager, s3, "Stores and retrieves files")
    Rel(assetManager, rabbitmq, "Sends processing messages")
    Rel(assetManager, postgres, "Stores metadata")
```

## Container Diagram

```mermaid
C4Container
    title Container Diagram - Assets Manager Modules

    Person(user, "End User")
    
    Container(webModule, "Web Module", "Spring Boot Web", "Handles file uploads, UI, and API")
    Container(workerModule, "Worker Module", "Spring Boot", "Processes thumbnails asynchronously")
    
    ContainerDb(s3, "AWS S3", "Object Storage", "Stores original files and thumbnails")
    ContainerQueue(mq, "RabbitMQ", "Message Broker", "Queues for async processing")
    ContainerDb(db, "PostgreSQL", "Database", "Stores file metadata")
    
    Rel(user, webModule, "Uses", "HTTPS")
    Rel(webModule, s3, "Upload/Download files", "AWS SDK")
    Rel(webModule, mq, "Send processing messages", "AMQP")
    Rel(webModule, db, "Read/Write metadata", "JPA/JDBC")
    Rel(workerModule, mq, "Consume messages", "AMQP")
    Rel(workerModule, s3, "Generate thumbnails", "AWS SDK")
    Rel(workerModule, db, "Update metadata", "JPA/JDBC")
```

## Component Diagram - Web Module

```mermaid
graph TB
    subgraph "Web Module"
        Controller[Controllers<br/>File Upload API<br/>Asset Viewer]
        StorageService[Storage Services<br/>AwsS3Service<br/>LocalFileStorageService]
        MessageProcessor[Message Processor<br/>BackupMessageProcessor]
        Repository[JPA Repositories<br/>ImageMetadata]
        Config[Configuration<br/>RabbitConfig<br/>AwsConfig]
    end
    
    subgraph "External Dependencies"
        UI[Thymeleaf Templates]
        S3[AWS S3<br/>File Storage]
        MQ[RabbitMQ<br/>Message Queue]
        DB[(PostgreSQL<br/>Metadata DB)]
    end
    
    Controller --> StorageService
    Controller --> UI
    StorageService --> S3
    StorageService --> MQ
    StorageService --> Repository
    MessageProcessor --> MQ
    Repository --> DB
    Config --> MQ
    Config --> S3
```

## Component Diagram - Worker Module

```mermaid
graph TB
    subgraph "Worker Module"
        Listener[Message Listeners<br/>Thumbnail Processing]
        FileProcessor[File Processing<br/>AbstractFileProcessingService<br/>Image/Video Processors]
        WorkerRepo[JPA Repositories<br/>Metadata Updates]
        WorkerConfig[Worker Configuration<br/>AMQP Setup]
    end
    
    subgraph "External Dependencies"
        WorkerMQ[RabbitMQ<br/>Processing Queue]
        WorkerS3[AWS S3<br/>Thumbnail Storage]
        WorkerDB[(PostgreSQL<br/>Metadata DB)]
    end
    
    Listener --> FileProcessor
    Listener --> WorkerMQ
    FileProcessor --> WorkerS3
    FileProcessor --> WorkerRepo
    WorkerRepo --> WorkerDB
    WorkerConfig --> WorkerMQ
```

## Data Flow - File Upload and Processing

```mermaid
sequenceDiagram
    participant User
    participant WebApp as Web Module
    participant S3 as AWS S3
    participant Queue as RabbitMQ
    participant Worker as Worker Module
    participant DB as PostgreSQL

    User->>WebApp: Upload file
    WebApp->>S3: Store original file
    S3-->>WebApp: File URL
    WebApp->>DB: Save metadata
    WebApp->>Queue: Send processing message
    WebApp-->>User: Upload confirmation
    
    Queue->>Worker: Processing message
    Worker->>S3: Download original
    Worker->>Worker: Generate thumbnail
    Worker->>S3: Upload thumbnail
    Worker->>DB: Update metadata
    Worker->>Queue: Acknowledge message
```

## Technology Stack

```mermaid
graph LR
    subgraph "Presentation Layer"
        A[Spring MVC<br/>Thymeleaf<br/>REST API]
    end
    
    subgraph "Business Layer"
        B[Spring Boot 3.2.1<br/>Java 17<br/>Service Classes]
    end
    
    subgraph "Integration Layer"
        C[AWS SDK for S3<br/>Spring AMQP<br/>Spring Data JPA]
    end
    
    subgraph "Data Layer"
        D[PostgreSQL<br/>AWS S3<br/>RabbitMQ]
    end
    
    A --> B
    B --> C
    C --> D
```

## Current Architecture - Deployment View

```mermaid
graph TB
    subgraph "Application Tier"
        Web[Web Module<br/>Port 8080<br/>Spring Boot]
        Worker[Worker Module<br/>Port 8081<br/>Spring Boot]
    end
    
    subgraph "Storage Tier"
        S3[AWS S3<br/>Files & Thumbnails]
    end
    
    subgraph "Messaging Tier"
        RabbitMQ[RabbitMQ<br/>localhost:5672]
    end
    
    subgraph "Database Tier"
        PostgreSQL[(PostgreSQL<br/>localhost:5432<br/>assets_manager DB)]
    end
    
    Web --> S3
    Web --> RabbitMQ
    Web --> PostgreSQL
    Worker --> S3
    Worker --> RabbitMQ
    Worker --> PostgreSQL
```

## Migration Impact - Services to Replace

```mermaid
graph LR
    subgraph "Current AWS Services"
        S3Current[AWS S3<br/>Object Storage<br/>SDK: software.amazon.awssdk]
    end
    
    subgraph "Current Messaging"
        RabbitCurrent[RabbitMQ<br/>AMQP Protocol<br/>Spring AMQP Starter]
    end
    
    subgraph "Current Database"
        PGCurrent[PostgreSQL<br/>localhost:5432]
    end
    
    subgraph "Target Azure Services"
        BlobStorage[Azure Blob Storage<br/>com.azure:azure-storage-blob]
        ServiceBus[Azure Service Bus<br/>JMS Integration]
        AzureDB[Azure Database<br/>for PostgreSQL]
    end
    
    S3Current -.Migration.-> BlobStorage
    RabbitCurrent -.Migration.-> ServiceBus
    PGCurrent -.Migration.-> AzureDB
    
    style S3Current fill:#ff9999
    style RabbitCurrent fill:#ff9999
    style PGCurrent fill:#ffcc99
    style BlobStorage fill:#99ff99
    style ServiceBus fill:#99ff99
    style AzureDB fill:#99ff99
```

## Key Findings

### Application Architecture
- **Type**: Multi-module Spring Boot application
- **Modules**: 
  - Web Module: Handles user interface and file upload API
  - Worker Module: Asynchronous thumbnail generation
- **Pattern**: Event-driven architecture with message-based processing
- **Framework**: Spring Boot 3.2.1 with Spring MVC, Spring Data JPA, Spring AMQP

### Dependencies
- **Storage**: AWS S3 (software.amazon.awssdk:s3:2.25.13)
- **Messaging**: RabbitMQ via Spring Boot AMQP starter
- **Database**: PostgreSQL with Spring Data JPA
- **UI**: Thymeleaf template engine
- **Build Tool**: Maven multi-module project

### Integration Points
1. **AWS S3 Integration** (11 incidents)
   - File upload and download operations
   - Thumbnail storage
   - URL generation for file access
   - Used in both web and worker modules

2. **RabbitMQ Integration** (9 incidents)
   - Asynchronous message processing
   - Queue-based thumbnail generation
   - Manual message acknowledgment
   - Both producer and consumer patterns

3. **PostgreSQL Integration** (2 incidents)
   - File metadata storage
   - JPA entity management
   - Connection configuration

### Configuration Concerns
- Hardcoded AWS credentials in configuration files (4 incidents)
- Localhost-based service endpoints
- Plain text database passwords
- No secrets management

### Recommended Migration Path
1. **Phase 1**: Replace RabbitMQ with Azure Service Bus (JMS integration)
2. **Phase 2**: Migrate AWS S3 to Azure Blob Storage
3. **Phase 3**: Configure Azure Database for PostgreSQL
4. **Phase 4**: Implement Azure Managed Identity for authentication
5. **Phase 5**: Add Azure Application Insights for monitoring

## Diagram Notes

- All diagrams use Mermaid syntax compatible with GitHub rendering
- C4 diagrams provide context and container views
- Component diagrams show internal structure of each module
- Sequence diagram illustrates the file processing workflow
- Migration impact diagram highlights services requiring changes

