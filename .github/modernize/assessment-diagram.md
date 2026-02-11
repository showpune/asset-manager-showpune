# Application Architecture Diagram

## Overview

This document contains the architecture diagram for the **Asset Manager** application, generated from the application assessment.

## Application Type

- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **Architecture**: Multi-module Maven project
- **Modules**: Web (UI & API) + Worker (Background Processing)

## C4 Context Diagram

```mermaid
C4Context
    title Asset Manager Application - System Context

    Person(user, "User", "Uploads and views images")
    
    System(assetManager, "Asset Manager", "Multi-module Spring Boot application for image management")
    
    System_Ext(s3, "AWS S3", "Cloud storage for images")
    System_Ext(rabbitmq, "RabbitMQ", "Message queue for async processing")
    System_Ext(postgres, "PostgreSQL", "Database for metadata")
    
    Rel(user, assetManager, "Uploads images, views gallery")
    Rel(assetManager, s3, "Stores and retrieves images")
    Rel(assetManager, rabbitmq, "Sends and receives messages")
    Rel(assetManager, postgres, "Stores image metadata")
```

## C4 Container Diagram

```mermaid
C4Container
    title Asset Manager Application - Container View

    Person(user, "User", "Application user")
    
    Container_Boundary(app, "Asset Manager Application") {
        Container(web, "Web Module", "Spring Boot, Thymeleaf", "Handles HTTP requests, file uploads, UI rendering")
        Container(worker, "Worker Module", "Spring Boot", "Processes images, generates thumbnails")
    }
    
    System_Ext(s3, "AWS S3", "Image storage")
    System_Ext(rabbitmq, "RabbitMQ", "Message broker")
    System_Ext(postgres, "PostgreSQL", "Metadata store")
    
    Rel(user, web, "Uses", "HTTPS")
    Rel(web, s3, "Uploads and downloads images")
    Rel(web, rabbitmq, "Sends processing messages")
    Rel(web, postgres, "Stores and queries metadata")
    Rel(worker, rabbitmq, "Receives processing messages")
    Rel(worker, s3, "Reads images, stores thumbnails")
    Rel(worker, postgres, "Updates processing status")
```

## Application Architecture Layers

```mermaid
graph TB
    subgraph "Web Module"
        UI[Presentation Layer<br/>Thymeleaf Templates<br/>Static Resources]
        Controller[Controller Layer<br/>HomeController<br/>S3Controller]
        WebService[Service Layer<br/>AwsS3Service<br/>LocalFileStorageService<br/>BackupMessageProcessor]
        WebRepo[Repository Layer<br/>ImageMetadataRepository]
    end
    
    subgraph "Worker Module"
        WorkerService[Service Layer<br/>S3FileProcessingService<br/>LocalFileProcessingService<br/>AbstractFileProcessingService]
        WorkerRepo[Repository Layer<br/>ImageMetadata Access]
    end
    
    subgraph "External Services"
        S3[AWS S3<br/>Image Storage]
        RMQ[RabbitMQ<br/>Message Queue]
        DB[PostgreSQL<br/>Metadata Database]
    end
    
    UI --> Controller
    Controller --> WebService
    WebService --> WebRepo
    WebService --> S3
    WebService --> RMQ
    WebRepo --> DB
    
    WorkerService --> WorkerRepo
    WorkerService --> S3
    WorkerService --> RMQ
    WorkerRepo --> DB
```

## Technology Stack

```mermaid
graph LR
    subgraph "Frontend"
        A[Thymeleaf Templates]
        B[HTML/CSS/JavaScript]
    end
    
    subgraph "Backend"
        C[Spring Boot 3.2.1]
        D[Spring MVC]
        E[Spring AMQP]
        F[Spring Data JPA]
    end
    
    subgraph "Storage & Messaging"
        G[AWS S3 SDK 2.25.13]
        H[RabbitMQ Client]
        I[PostgreSQL Driver]
    end
    
    subgraph "Build & Tools"
        J[Maven]
        K[Java 17]
        L[Lombok]
    end
```

## Data Flow

### Upload Flow

```mermaid
sequenceDiagram
    participant User
    participant WebUI as Web UI
    participant Controller as S3Controller
    participant Service as AwsS3Service
    participant S3 as AWS S3
    participant Queue as RabbitMQ
    participant DB as PostgreSQL

    User->>WebUI: Upload Image
    WebUI->>Controller: POST /upload
    Controller->>Service: uploadFile(multipartFile)
    Service->>S3: Upload image
    S3-->>Service: Upload confirmation
    Service->>DB: Save metadata
    Service->>Queue: Send processing message
    Queue-->>Service: Message sent
    Service-->>Controller: Upload successful
    Controller-->>WebUI: Redirect to gallery
    WebUI-->>User: Show success message
```

### Processing Flow

```mermaid
sequenceDiagram
    participant Queue as RabbitMQ
    participant Worker as Worker Service
    participant S3 as AWS S3
    participant DB as PostgreSQL

    Queue->>Worker: Image processing message
    Worker->>S3: Download original image
    S3-->>Worker: Image data
    Worker->>Worker: Generate thumbnail
    Worker->>S3: Upload thumbnail
    S3-->>Worker: Upload confirmation
    Worker->>DB: Update processing status
    DB-->>Worker: Status updated
```

## Configuration Profiles

The application supports multiple configuration profiles:

```mermaid
graph LR
    A[Application Profiles] --> B[dev]
    A --> C[default/production]
    
    B --> D[Local File Storage]
    B --> E[Local Services]
    
    C --> F[AWS S3 Storage]
    C --> G[RabbitMQ]
    C --> H[PostgreSQL]
```

- **dev profile**: Uses local file storage, no cloud dependencies
- **default profile**: Uses AWS S3, RabbitMQ, and PostgreSQL

## Key Components

### Web Module Components

| Component | Purpose | Technology |
|-----------|---------|------------|
| HomeController | Handles main UI requests | Spring MVC |
| S3Controller | Manages file operations | Spring MVC |
| AwsS3Service | AWS S3 integration | AWS SDK |
| LocalFileStorageService | Local file storage (dev) | Java NIO |
| BackupMessageProcessor | Message backup handling | Spring AMQP |
| ImageMetadataRepository | Database access | Spring Data JPA |

### Worker Module Components

| Component | Purpose | Technology |
|-----------|---------|------------|
| S3FileProcessingService | S3 file processing | AWS SDK |
| LocalFileProcessingService | Local file processing (dev) | Java NIO |
| AbstractFileProcessingService | Base processing logic | Spring Boot |

## External Dependencies

### Cloud Storage
- **AWS S3** (software.amazon.awssdk:s3:2.25.13)
  - Image storage
  - Thumbnail storage
  - File retrieval

### Messaging
- **RabbitMQ** (spring-boot-starter-amqp)
  - Async image processing queue
  - Message-based communication between web and worker

### Database
- **PostgreSQL** (org.postgresql:postgresql)
  - Image metadata storage
  - Processing status tracking
  - JPA/Hibernate ORM

## Security Considerations

⚠️ **Current Security Issues** (from Assessment):

1. **Hardcoded AWS Credentials**: Access keys in application.properties
2. **Hardcoded Database Credentials**: Username/password in configuration
3. **Hardcoded RabbitMQ Credentials**: Default guest credentials

🎯 **Recommended Security Improvements**:
- Implement Azure Managed Identity
- Use Azure Key Vault for secrets
- Remove all hardcoded credentials

## Migration Notes

This architecture assessment identifies the following components requiring migration to Azure:

| Current | Azure Target | Effort |
|---------|--------------|--------|
| AWS S3 | Azure Blob Storage | High (7 points) |
| RabbitMQ | Azure Service Bus | Medium (5 points) |
| PostgreSQL | Azure Database for PostgreSQL | Low (1 point) |
| Hardcoded Credentials | Azure Managed Identity + Key Vault | Medium (3 points) |
| No Monitoring | Azure Application Insights | Low (1 point) |

**Total Estimated Effort**: 18 story points

## Architecture Strengths

✅ **Good Practices**:
- Clear separation of concerns (web vs. worker)
- Profile-based configuration for different environments
- Async processing pattern for long-running tasks
- Repository pattern for data access
- Multi-module Maven structure

⚠️ **Areas for Improvement**:
- Remove cloud provider-specific naming (S3Controller, S3StorageItem)
- Implement proper secrets management
- Add monitoring and observability
- Upgrade to latest Spring Boot LTS version
- Add comprehensive error handling

## Deployment Architecture

```mermaid
graph TB
    subgraph "Azure Cloud (Target)"
        subgraph "Compute"
            WebApp[Azure App Service<br/>Web Module]
            WorkerApp[Azure App Service<br/>Worker Module]
        end
        
        subgraph "Storage"
            Blob[Azure Blob Storage<br/>Images & Thumbnails]
        end
        
        subgraph "Messaging"
            ServiceBus[Azure Service Bus<br/>Processing Queue]
        end
        
        subgraph "Database"
            PostgreSQL[Azure Database for PostgreSQL<br/>Metadata]
        end
        
        subgraph "Monitoring"
            AppInsights[Application Insights<br/>Telemetry & Logs]
        end
        
        subgraph "Security"
            ManagedIdentity[Managed Identity<br/>Authentication]
            KeyVault[Key Vault<br/>Secrets Management]
        end
    end
    
    WebApp --> Blob
    WebApp --> ServiceBus
    WebApp --> PostgreSQL
    WorkerApp --> Blob
    WorkerApp --> ServiceBus
    WorkerApp --> PostgreSQL
    
    WebApp --> AppInsights
    WorkerApp --> AppInsights
    
    WebApp --> ManagedIdentity
    WorkerApp --> ManagedIdentity
    ManagedIdentity --> KeyVault
```

## Summary

The Asset Manager is a well-structured Spring Boot application with clear separation between web and worker modules. The architecture follows good practices with async processing and profile-based configuration. The main migration work involves replacing AWS services with Azure equivalents and implementing proper security through Managed Identity and Key Vault.

**Cloud Readiness Score**: 62/100

**Primary Migration Tasks**:
1. Migrate AWS S3 to Azure Blob Storage
2. Migrate RabbitMQ to Azure Service Bus  
3. Implement Azure Managed Identity for authentication
4. Configure Azure Database for PostgreSQL
5. Add Azure Application Insights for monitoring
6. Remove all hardcoded credentials
