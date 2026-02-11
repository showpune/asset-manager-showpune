# Architecture Diagram - Asset Manager Application

## Overview

This document provides a visual representation of the Asset Manager application architecture based on the assessment analysis.

## Application Information

- **Name**: Asset Manager
- **Type**: Multi-module Spring Boot Application
- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **Build Tool**: Maven
- **Modules**: Web (UI & API), Worker (Background Processing)

## High-Level Architecture

```mermaid
C4Context
    title System Context Diagram - Asset Manager Application

    Person(user, "User", "Uploads and manages image assets")
    
    System_Boundary(app, "Asset Manager System") {
        Container(web, "Web Application", "Spring Boot 3.2.1", "Provides UI and REST APIs for asset management")
        Container(worker, "Worker Application", "Spring Boot 3.2.1", "Processes images and generates thumbnails")
    }
    
    System_Ext(s3, "AWS S3", "Object Storage", "Stores original images and thumbnails")
    System_Ext(rabbitmq, "RabbitMQ", "Message Queue", "Async message processing")
    System_Ext(postgres, "PostgreSQL", "Relational Database", "Stores image metadata")
    
    Rel(user, web, "Uploads images, Views gallery", "HTTPS")
    Rel(web, s3, "Stores and retrieves images", "AWS SDK")
    Rel(web, rabbitmq, "Sends processing messages", "AMQP")
    Rel(web, postgres, "Stores metadata", "JDBC")
    Rel(worker, rabbitmq, "Receives processing messages", "AMQP")
    Rel(worker, s3, "Downloads originals, Uploads thumbnails", "AWS SDK")
    Rel(worker, postgres, "Updates metadata", "JDBC")
```

## Component Architecture

```mermaid
graph TB
    subgraph "Web Module"
        UI[Thymeleaf UI<br/>Upload & Gallery Views]
        Controller[REST Controllers<br/>File Upload/Download APIs]
        WebService[Business Services<br/>AwsS3Service<br/>LocalFileStorageService]
        WebConfig[Configuration<br/>AwsS3Config<br/>RabbitConfig]
    end
    
    subgraph "Worker Module"
        Listener[Message Listener<br/>AbstractFileProcessingService]
        WorkerService[Processing Services<br/>S3FileProcessingService<br/>LocalFileProcessingService]
        ImageProcessor[Image Processing<br/>Thumbnail Generation]
        WorkerConfig[Configuration<br/>AwsS3Config<br/>RabbitConfig]
    end
    
    subgraph "External Services"
        S3[AWS S3<br/>Image Storage]
        RabbitMQ[RabbitMQ<br/>Message Queue]
        DB[(PostgreSQL<br/>Metadata DB)]
    end
    
    UI --> Controller
    Controller --> WebService
    WebService --> WebConfig
    WebService --> S3
    WebService --> RabbitMQ
    WebService --> DB
    
    RabbitMQ --> Listener
    Listener --> WorkerService
    WorkerService --> ImageProcessor
    WorkerService --> S3
    WorkerService --> DB
    WorkerService --> WorkerConfig
```

## Data Flow

```mermaid
sequenceDiagram
    participant User
    participant Web as Web Application
    participant Queue as RabbitMQ
    participant Worker as Worker Application
    participant S3 as AWS S3
    participant DB as PostgreSQL

    User->>Web: Upload Image
    Web->>S3: Store Original Image
    S3-->>Web: Upload Success
    Web->>DB: Save Image Metadata
    Web->>Queue: Send Processing Message
    Web-->>User: Upload Complete
    
    Queue->>Worker: Deliver Message
    Worker->>S3: Download Original
    Worker->>Worker: Generate Thumbnail
    Worker->>S3: Upload Thumbnail
    Worker->>DB: Update Metadata
    Worker->>Queue: Acknowledge Message
    
    User->>Web: View Gallery
    Web->>DB: Fetch Metadata
    Web->>S3: Get Image URLs
    Web-->>User: Display Images
```

## Technology Stack

### Web Module
- **Framework**: Spring Boot 3.2.1
- **View Layer**: Thymeleaf
- **REST Layer**: Spring Web MVC
- **Data Access**: Spring Data JPA with Hibernate
- **Messaging**: Spring AMQP (RabbitMQ)
- **Cloud Storage**: AWS SDK for Java 2.25.13
- **Database Driver**: PostgreSQL JDBC

### Worker Module
- **Framework**: Spring Boot 3.2.1
- **Data Access**: Spring Data JPA with Hibernate
- **Messaging**: Spring AMQP (RabbitMQ)
- **Cloud Storage**: AWS SDK for Java 2.25.13
- **Image Processing**: Java ImageIO
- **Database Driver**: PostgreSQL JDBC

### Infrastructure
- **Object Storage**: AWS S3
- **Message Queue**: RabbitMQ
- **Database**: PostgreSQL
- **Build Tool**: Maven

## Key Dependencies

| Dependency | Version | Purpose | Used In |
|------------|---------|---------|---------|
| Spring Boot | 3.2.1 | Application Framework | Web, Worker |
| AWS SDK S3 | 2.25.13 | Cloud Storage | Web, Worker |
| Spring AMQP | (Spring Boot) | Message Queue Integration | Web, Worker |
| PostgreSQL | (Runtime) | Database Driver | Web, Worker |
| Thymeleaf | (Spring Boot) | Template Engine | Web |
| Spring Data JPA | (Spring Boot) | Data Access | Web, Worker |
| Lombok | (Optional) | Code Generation | Web, Worker |

## Configuration Profiles

The application supports multiple configuration profiles:

- **dev profile**: Uses local file storage instead of AWS S3
- **default profile**: Uses AWS S3 for cloud storage

Profile-specific services:
- `LocalFileStorageService` - Active with `dev` profile
- `AwsS3Service` - Active without `dev` profile (default)

## Storage Layer Design

```mermaid
classDiagram
    class StorageService {
        <<interface>>
        +listObjects() List~S3StorageItem~
        +uploadObject(file) void
        +getObject(key) InputStream
        +deleteObject(key) void
        +getStorageType() String
    }
    
    class AwsS3Service {
        +Profile: !dev
        -S3Client s3Client
        -RabbitTemplate rabbitTemplate
        -ImageMetadataRepository repository
    }
    
    class LocalFileStorageService {
        +Profile: dev
        -Path uploadDir
        -RabbitTemplate rabbitTemplate
        -ImageMetadataRepository repository
    }
    
    StorageService <|.. AwsS3Service : implements
    StorageService <|.. LocalFileStorageService : implements
```

## Message Processing Architecture

```mermaid
graph LR
    WebApp[Web Application]
    Queue[RabbitMQ Queue<br/>image-processing]
    Worker[Worker Application]
    
    WebApp -->|RabbitTemplate<br/>convertAndSend| Queue
    Queue -->|@RabbitListener<br/>Manual ACK| Worker
    
    style Queue fill:#ff9,stroke:#333,stroke-width:2px
```

### Message Structure

```json
{
  "key": "uuid-filename.jpg",
  "contentType": "image/jpeg",
  "storageType": "s3",
  "size": 1048576
}
```

## Database Schema

```mermaid
erDiagram
    ImageMetadata {
        string id PK
        string filename
        string contentType
        long size
        string s3Key
        string s3Url
        timestamp uploadedAt
        timestamp createdAt
        timestamp updatedAt
    }
```

## Deployment Architecture (Current State)

```mermaid
graph TB
    subgraph "Application Tier"
        WebApp[Web Application<br/>Port 8080]
        WorkerApp[Worker Application<br/>Port 8081]
    end
    
    subgraph "Data Tier"
        PostgreSQL[(PostgreSQL<br/>Port 5432)]
    end
    
    subgraph "Infrastructure Tier"
        RabbitMQ[RabbitMQ<br/>Port 5672]
    end
    
    subgraph "Cloud Tier"
        S3[AWS S3<br/>Bucket]
    end
    
    WebApp --> PostgreSQL
    WebApp --> RabbitMQ
    WebApp --> S3
    WorkerApp --> PostgreSQL
    WorkerApp --> RabbitMQ
    WorkerApp --> S3
```

## Security Configuration

### Authentication
- **AWS S3**: Access Key & Secret Key (configured in application.properties)
- **RabbitMQ**: Username & Password (guest/guest for local dev)
- **PostgreSQL**: Username & Password (postgres/postgres for local dev)

⚠️ **Security Concerns**:
- Credentials stored in plain text in application.properties
- No secrets management solution
- No managed identity or IAM roles

## Migration Considerations

### AWS-Specific Components to Migrate

1. **AWS S3 SDK** (software.amazon.awssdk:s3)
   - Used in: AwsS3Service, S3FileProcessingService
   - Target: Azure Blob Storage SDK

2. **RabbitMQ** (Spring AMQP)
   - Used in: RabbitConfig, Message processing
   - Target: Azure Service Bus

3. **Credentials Management**
   - Current: Plain text in application.properties
   - Target: Azure Managed Identity / Key Vault

### Architecture Strengths for Migration

✅ **Profile-based Configuration**: Easy to add Azure profiles alongside existing ones
✅ **StorageService Interface**: Clean abstraction for adding Azure Blob Storage implementation
✅ **Module Separation**: Web and Worker can be migrated independently
✅ **Spring Boot Framework**: Good support for Azure services via Spring Cloud Azure

## Architecture Patterns Used

- **Multi-module Maven Project**: Separation of concerns between web and worker
- **Interface-based Design**: StorageService abstraction for storage implementations
- **Profile-based Configuration**: Environment-specific implementations
- **Message-driven Architecture**: Async processing via message queue
- **Repository Pattern**: Data access through Spring Data JPA repositories
- **Dependency Injection**: Spring IoC container manages all components

## Key Observations

1. **Cloud-Ready Architecture**: Application already uses cloud storage and messaging, making Azure migration straightforward
2. **Good Abstraction**: StorageService interface allows adding Azure implementation without changing consumers
3. **Async Processing**: Message queue pattern is well-implemented and can be migrated to Azure Service Bus
4. **Profile Support**: Profile-based configuration makes it easy to support multiple environments
5. **Security Gaps**: Hardcoded credentials need to be addressed with Azure Key Vault or Managed Identity

## Next Steps

Based on this architecture analysis:

1. ✅ Create Azure Blob Storage implementation of StorageService interface
2. ✅ Migrate RabbitMQ configuration to Azure Service Bus
3. ✅ Implement Azure Managed Identity for authentication
4. ✅ Configure Azure Key Vault for secrets management
5. ✅ Add Application Insights for monitoring
6. ✅ Migrate PostgreSQL to Azure Database for PostgreSQL

---

*This diagram was generated based on automated assessment of the codebase structure, dependencies, and configuration files.*
