# Asset Manager Application - Architecture Diagram

## Overview

This document provides visual representations of the Asset Manager application architecture based on the assessment results. The application is a multi-module Spring Boot system for managing image assets with asynchronous thumbnail generation.

## Application Profile

- **Name**: Asset Manager Application
- **Type**: Java Spring Boot Multi-Module
- **Version**: 0.0.1-SNAPSHOT
- **Framework**: Spring Boot 3.2.1
- **Java Version**: 17
- **Build Tool**: Maven

---

## C4 Model - System Context Diagram

This diagram shows the Asset Manager system and its interactions with external systems and users.

```mermaid
graph TB
    User[User]
    WebApp[Asset Manager Web Application]
    WorkerApp[Asset Manager Worker Application]
    S3[AWS S3 Storage]
    RabbitMQ[RabbitMQ Message Broker]
    PostgreSQL[(PostgreSQL Database)]
    
    User -->|Upload Images| WebApp
    User -->|View Images| WebApp
    WebApp -->|Store Original Images| S3
    WebApp -->|Send Processing Messages| RabbitMQ
    WebApp -->|Store Metadata| PostgreSQL
    
    RabbitMQ -->|Process Image Queue| WorkerApp
    WorkerApp -->|Download Original| S3
    WorkerApp -->|Upload Thumbnail| S3
    WorkerApp -->|Update Status| PostgreSQL
    
    style WebApp fill:#e1f5ff
    style WorkerApp fill:#e1f5ff
    style S3 fill:#ff9999
    style RabbitMQ fill:#ffcc99
    style PostgreSQL fill:#99ccff
```

---

## Application Architecture - Module Structure

This diagram illustrates the multi-module structure and key components within each module.

```mermaid
graph TB
    subgraph "Asset Manager System"
        subgraph "Web Module Port 8080"
            WebUI[Thymeleaf UI Layer]
            WebController[Controllers]
            WebService[Services]
            WebConfig[Configuration]
            
            WebUI --> WebController
            WebController --> WebService
            WebService --> WebConfig
        end
        
        subgraph "Worker Module Port 8081"
            WorkerListener[Message Listener]
            WorkerService[Processing Services]
            WorkerConfig[Configuration]
            
            WorkerListener --> WorkerService
            WorkerService --> WorkerConfig
        end
        
        subgraph "Shared Components"
            Models[Data Models]
            Repositories[JPA Repositories]
        end
        
        WebService --> Models
        WebService --> Repositories
        WorkerService --> Models
        WorkerService --> Repositories
    end
    
    style WebUI fill:#b3d9ff
    style WebController fill:#b3d9ff
    style WebService fill:#b3d9ff
    style WorkerListener fill:#d9b3ff
    style WorkerService fill:#d9b3ff
```

---

## Technology Stack

```mermaid
graph LR
    subgraph "Presentation Layer"
        Thymeleaf[Thymeleaf Templates]
        REST[REST Controllers]
    end
    
    subgraph "Application Layer"
        SpringBoot[Spring Boot 3.2.1]
        SpringWeb[Spring Web MVC]
        SpringAMQP[Spring AMQP]
    end
    
    subgraph "Data Layer"
        SpringDataJPA[Spring Data JPA]
        Hibernate[Hibernate ORM]
    end
    
    subgraph "External Services"
        AWSSDK[AWS SDK S3 v2.25.13]
        RabbitMQClient[RabbitMQ Client]
        PostgreSQLDriver[PostgreSQL JDBC Driver]
    end
    
    Thymeleaf --> SpringWeb
    REST --> SpringWeb
    SpringWeb --> SpringBoot
    SpringAMQP --> SpringBoot
    SpringDataJPA --> SpringBoot
    SpringDataJPA --> Hibernate
    
    SpringBoot --> AWSSDK
    SpringBoot --> RabbitMQClient
    Hibernate --> PostgreSQLDriver
    
    style SpringBoot fill:#6db33f
    style AWSSDK fill:#ff9900
    style RabbitMQClient fill:#ff6600
    style PostgreSQLDriver fill:#336791
```

---

## Data Flow - Image Upload and Processing

This diagram shows the complete flow of an image from upload to thumbnail generation.

```mermaid
sequenceDiagram
    participant User
    participant WebUI as Web UI
    participant WebService as Web Service
    participant S3 as AWS S3
    participant DB as PostgreSQL
    participant Queue as RabbitMQ
    participant Worker as Worker Service
    
    User->>WebUI: Upload Image
    WebUI->>WebService: Process Upload Request
    WebService->>S3: Store Original Image
    S3-->>WebService: Return S3 Key
    WebService->>DB: Save Image Metadata
    DB-->>WebService: Confirm Save
    WebService->>Queue: Send Processing Message
    Queue-->>WebService: Message Queued
    WebService-->>WebUI: Upload Complete
    WebUI-->>User: Show Success
    
    Queue->>Worker: Deliver Processing Message
    Worker->>S3: Download Original Image
    S3-->>Worker: Return Image Data
    Worker->>Worker: Generate Thumbnail
    Worker->>S3: Upload Thumbnail
    S3-->>Worker: Confirm Upload
    Worker->>DB: Update Metadata Status
    DB-->>Worker: Confirm Update
    Worker->>Queue: Acknowledge Message
```

---

## Component Details

### Web Module Components

| Component | Type | Responsibility |
|-----------|------|----------------|
| **HomeController** | Controller | Handles home page requests and navigation |
| **S3Controller** | Controller | Manages file upload and storage operations |
| **AwsS3Service** | Service | Implements S3 file operations (upload, download, delete) |
| **LocalFileStorageService** | Service | Provides local file storage for dev/test |
| **BackupMessageProcessor** | Service | Handles message publishing to RabbitMQ |
| **ImageMetadataRepository** | Repository | JPA repository for image metadata CRUD |
| **AwsS3Config** | Configuration | Configures AWS S3 client with credentials |
| **RabbitConfig** | Configuration | Configures RabbitMQ queues and connection |

### Worker Module Components

| Component | Type | Responsibility |
|-----------|------|----------------|
| **Message Listener** | Listener | Consumes messages from RabbitMQ queue |
| **S3FileProcessingService** | Service | Processes images from S3 storage |
| **LocalFileProcessingService** | Service | Processes local files for dev/test |
| **AbstractFileProcessingService** | Service | Base class for file processing logic |
| **FileProcessor** | Service | Core thumbnail generation logic |
| **AwsS3Config** | Configuration | Worker module S3 client configuration |
| **RabbitConfig** | Configuration | Worker module RabbitMQ configuration |

---

## Deployment Architecture

```mermaid
graph TB
    subgraph "Current Deployment On-Premises or IaaS"
        subgraph "Application Servers"
            WebApp[Web Module JAR<br/>Port 8080]
            WorkerApp[Worker Module JAR<br/>Port 8081]
        end
        
        subgraph "External Services"
            S3Service[AWS S3<br/>Object Storage]
            RabbitMQService[RabbitMQ<br/>Message Broker<br/>Port 5672]
            PostgreSQLDB[(PostgreSQL<br/>Database<br/>Port 5432)]
        end
        
        WebApp -->|AMQP| RabbitMQService
        WorkerApp -->|AMQP| RabbitMQService
        WebApp -->|HTTPS| S3Service
        WorkerApp -->|HTTPS| S3Service
        WebApp -->|JDBC| PostgreSQLDB
        WorkerApp -->|JDBC| PostgreSQLDB
    end
    
    LoadBalancer[Load Balancer]
    Users[End Users]
    
    Users --> LoadBalancer
    LoadBalancer --> WebApp
    
    style WebApp fill:#e1f5ff
    style WorkerApp fill:#e1f5ff
    style S3Service fill:#ff9999
    style RabbitMQService fill:#ffcc99
    style PostgreSQLDB fill:#99ccff
```

---

## Target Azure Architecture (Post-Migration)

```mermaid
graph TB
    subgraph "Azure Cloud"
        subgraph "Compute"
            AppService[Azure App Service<br/>Web Module]
            ContainerApp[Azure Container Apps<br/>Worker Module]
        end
        
        subgraph "Storage"
            BlobStorage[Azure Blob Storage<br/>Container: images]
        end
        
        subgraph "Messaging"
            ServiceBus[Azure Service Bus<br/>Queue: image-processing]
        end
        
        subgraph "Database"
            AzurePostgreSQL[(Azure Database<br/>for PostgreSQL)]
        end
        
        subgraph "Monitoring"
            AppInsights[Application Insights]
        end
        
        subgraph "Security"
            ManagedIdentity[Managed Identity]
            KeyVault[Azure Key Vault]
        end
        
        AppService -->|Messages| ServiceBus
        ContainerApp -->|Messages| ServiceBus
        AppService -->|Blob API| BlobStorage
        ContainerApp -->|Blob API| BlobStorage
        AppService -->|Connection| AzurePostgreSQL
        ContainerApp -->|Connection| AzurePostgreSQL
        
        AppService -.->|Telemetry| AppInsights
        ContainerApp -.->|Telemetry| AppInsights
        
        AppService -->|Authentication| ManagedIdentity
        ContainerApp -->|Authentication| ManagedIdentity
        ManagedIdentity -->|Access| BlobStorage
        ManagedIdentity -->|Access| ServiceBus
        ManagedIdentity -->|Access| AzurePostgreSQL
        ManagedIdentity -->|Secrets| KeyVault
    end
    
    Internet[Internet Users]
    Internet --> AppService
    
    style AppService fill:#0078d4
    style ContainerApp fill:#0078d4
    style BlobStorage fill:#0078d4
    style ServiceBus fill:#0078d4
    style AzurePostgreSQL fill:#0078d4
    style AppInsights fill:#68217a
    style ManagedIdentity fill:#68217a
```

---

## Migration Impact Areas

### High Priority Migration Areas

```mermaid
graph LR
    subgraph "Storage Migration"
        S3[AWS S3 SDK<br/>11 Incidents<br/>7 Story Points]
    end
    
    subgraph "Messaging Migration"
        RabbitMQ[Spring AMQP RabbitMQ<br/>9 Incidents<br/>5 Story Points]
    end
    
    subgraph "Security Migration"
        Credentials[Hardcoded Credentials<br/>4 Incidents<br/>3 Story Points]
    end
    
    subgraph "Database Migration"
        DB[PostgreSQL Config<br/>2 Incidents<br/>1 Story Point]
    end
    
    subgraph "Framework Updates"
        Framework[Spring Boot Version<br/>Monitoring Integration<br/>8 Incidents<br/>2 Story Points]
    end
    
    S3 -.->|Replace with| AzureBlob[Azure Blob Storage]
    RabbitMQ -.->|Replace with| ServiceBus[Azure Service Bus]
    Credentials -.->|Replace with| ManagedIdentity[Managed Identity]
    DB -.->|Migrate to| AzureDB[Azure PostgreSQL]
    Framework -.->|Add| Monitoring[App Insights]
    
    style S3 fill:#ff9999
    style RabbitMQ fill:#ffcc99
    style Credentials fill:#ff6666
    style DB fill:#99ccff
    style Framework fill:#ffff99
    style AzureBlob fill:#0078d4
    style ServiceBus fill:#0078d4
    style ManagedIdentity fill:#68217a
    style AzureDB fill:#0078d4
    style Monitoring fill:#68217a
```

---

## Key Findings Summary

### Application Characteristics

- **Architecture Pattern**: Multi-module microservices with async processing
- **Communication**: REST (user-facing) + Message Queue (inter-module)
- **Storage Pattern**: Object storage for files, relational DB for metadata
- **Deployment**: Two independent deployable modules

### Migration Complexity

- **Cloud Readiness Score**: 62/100 (Moderate)
- **Total Incidents**: 34
- **Estimated Effort**: 18 Story Points (3-4 weeks)
- **Critical Issues**: 7 (AWS S3, RabbitMQ, Credentials)
- **High Priority Issues**: 15 (Service implementations, configurations)

### Technology Dependencies

| Category | Current Technology | Target Azure Service | Migration Effort |
|----------|-------------------|---------------------|------------------|
| **Storage** | AWS S3 | Azure Blob Storage | High (7 points) |
| **Messaging** | RabbitMQ | Azure Service Bus | High (5 points) |
| **Database** | PostgreSQL | Azure Database for PostgreSQL | Low (1 point) |
| **Authentication** | Static Credentials | Managed Identity | Medium (3 points) |
| **Monitoring** | None | Application Insights | Medium (2 points) |

---

## Notes

- This architecture diagram was generated based on code analysis and assessment results
- The application follows good separation of concerns with distinct web and worker modules
- LocalFileStorageService exists for development/testing and should be preserved
- The async processing pattern with message queues is well-suited for cloud deployment
- Both modules share common data models and repository interfaces for consistency

---

## Next Steps

1. Review this architecture with stakeholders
2. Validate migration priorities based on business requirements
3. Create detailed migration plan for each phase
4. Set up Azure resources and test environments
5. Begin with Phase 1: Managed Identity and Service Bus migration
6. Continue with Phase 2: Blob Storage migration
7. Complete with Phase 3: Database configuration and monitoring

---

**Generated**: 2026-02-11T06:07:42.203Z  
**Tool**: Manual Architecture Analysis  
**Version**: 1.0.0
