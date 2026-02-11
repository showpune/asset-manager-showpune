# Asset Manager - Architecture Diagrams

This document contains architecture diagrams for the Asset Manager application, generated from the assessment analysis.

## Table of Contents
- [Current Architecture Overview](#current-architecture-overview)
- [Application Component View](#application-component-view)
- [Data Flow Diagram](#data-flow-diagram)
- [Migration Target Architecture](#migration-target-architecture)
- [Technology Stack](#technology-stack)

---

## Current Architecture Overview

This diagram shows the high-level architecture of the Asset Manager application with its current AWS dependencies.

```mermaid
C4Context
    title Asset Manager - Current Architecture (AWS-based)
    
    Person(user, "User", "Uploads and manages images")
    
    System_Boundary(app, "Asset Manager Application") {
        Container(web, "Web Module", "Spring Boot 3.2.1", "Handles file uploads, REST APIs, and UI")
        Container(worker, "Worker Module", "Spring Boot 3.2.1", "Processes image thumbnails asynchronously")
    }
    
    System_Ext(s3, "AWS S3", "Object Storage", "Stores images and thumbnails")
    System_Ext(rabbitmq, "RabbitMQ", "Message Queue", "Asynchronous communication")
    System_Ext(postgres, "PostgreSQL", "Database", "Stores image metadata")
    
    Rel(user, web, "Uploads images, views gallery", "HTTPS")
    Rel(web, s3, "Stores objects", "AWS SDK")
    Rel(web, rabbitmq, "Sends processing messages", "AMQP")
    Rel(web, postgres, "Saves metadata", "JDBC")
    Rel(worker, rabbitmq, "Consumes messages", "AMQP")
    Rel(worker, s3, "Reads images, writes thumbnails", "AWS SDK")
    Rel(worker, postgres, "Updates metadata", "JDBC")
```

---

## Application Component View

This diagram shows the internal structure and components of each module.

```mermaid
graph TB
    subgraph "Web Module"
        WC[Controllers]
        WS[Services]
        WR[Repositories]
        WCF[Configuration]
        WM[Models]
        
        WC -->|uses| WS
        WS -->|uses| WR
        WS -->|configured by| WCF
        WS -->|works with| WM
    end
    
    subgraph "Worker Module"
        WKS[Processing Services]
        WKR[Repositories]
        WKCF[Configuration]
        WKM[Models]
        
        WKS -->|uses| WKR
        WKS -->|configured by| WKCF
        WKS -->|works with| WKM
    end
    
    subgraph "External Services"
        S3[(AWS S3)]
        MQ[RabbitMQ]
        DB[(PostgreSQL)]
    end
    
    WS -->|AwsS3Service| S3
    WS -->|RabbitTemplate| MQ
    WR -->|JPA| DB
    WKS -->|S3FileProcessingService| S3
    WKS -->|RabbitListener| MQ
    WKR -->|JPA| DB
    
    style WC fill:#e1f5ff
    style WS fill:#e1f5ff
    style WKS fill:#fff4e1
    style S3 fill:#ff9999
    style MQ fill:#ff9999
    style DB fill:#99ccff
```

---

## Data Flow Diagram

This diagram illustrates the complete flow of data through the system when a user uploads an image.

```mermaid
sequenceDiagram
    actor User
    participant Web as Web Module
    participant S3 as AWS S3
    participant Queue as RabbitMQ
    participant Worker as Worker Module
    participant DB as PostgreSQL
    
    User->>Web: Upload image file
    Web->>S3: Upload original image
    S3-->>Web: Upload confirmation
    Web->>DB: Save image metadata
    DB-->>Web: Metadata saved
    Web->>Queue: Send processing message
    Queue-->>Web: Message queued
    Web-->>User: Upload successful
    
    Queue->>Worker: Deliver processing message
    Worker->>S3: Download original image
    S3-->>Worker: Image data
    Worker->>Worker: Generate thumbnail
    Worker->>S3: Upload thumbnail
    S3-->>Worker: Upload confirmation
    Worker->>DB: Update metadata with thumbnail URL
    DB-->>Worker: Update complete
    Worker->>Queue: Acknowledge message
    
    Note over User,DB: User can now view both original and thumbnail
```

---

## Migration Target Architecture

This diagram shows the target Azure architecture after migration.

```mermaid
C4Context
    title Asset Manager - Target Architecture (Azure)
    
    Person(user, "User", "Uploads and manages images")
    
    System_Boundary(azure, "Azure Cloud Platform") {
        Container(web, "Web App Service", "Spring Boot 3.4.x", "Handles uploads and API")
        Container(worker, "Worker App Service", "Spring Boot 3.4.x", "Processes thumbnails")
        Container(blob, "Azure Blob Storage", "Hot tier", "Stores images and thumbnails")
        Container(servicebus, "Azure Service Bus", "Standard tier", "Message queue")
        Container(db, "PostgreSQL Flexible Server", "General Purpose", "Image metadata")
        Container(insights, "Application Insights", "Monitoring", "Telemetry and logs")
        Container(identity, "Managed Identity", "Security", "Passwordless authentication")
    }
    
    Rel(user, web, "Uses application", "HTTPS")
    Rel(web, blob, "Store and retrieve", "Azure SDK")
    Rel(web, servicebus, "Send messages", "Azure SDK")
    Rel(web, db, "Save metadata", "JDBC SSL")
    Rel(web, insights, "Send telemetry", "Auto")
    Rel(worker, servicebus, "Receive messages", "Azure SDK")
    Rel(worker, blob, "Process images", "Azure SDK")
    Rel(worker, db, "Update metadata", "JDBC SSL")
    Rel(worker, insights, "Send telemetry", "Auto")
    Rel(identity, web, "Authenticates", "")
    Rel(identity, worker, "Authenticates", "")
    Rel(identity, blob, "Authorizes", "")
    Rel(identity, servicebus, "Authorizes", "")
```

---

## Technology Stack

### Current Stack (AWS-based)

```mermaid
graph LR
    subgraph "Application Layer"
        A[Spring Boot 3.2.1]
        B[Java 17]
        C[Thymeleaf]
        D[Spring Web MVC]
    end
    
    subgraph "Data Layer"
        E[Spring Data JPA]
        F[Hibernate]
        G[PostgreSQL Driver]
    end
    
    subgraph "Integration Layer"
        H[AWS SDK 2.25.13]
        I[Spring AMQP]
        J[RabbitMQ Client]
        K[Jackson JSON]
    end
    
    subgraph "Infrastructure"
        L[AWS S3]
        M[RabbitMQ Server]
        N[PostgreSQL DB]
    end
    
    A --> E
    A --> H
    A --> I
    E --> G
    H --> L
    I --> J
    J --> M
    G --> N
    
    style L fill:#ff9999
    style M fill:#ff9999
    style N fill:#99ccff
```

### Target Stack (Azure)

```mermaid
graph LR
    subgraph "Application Layer"
        A[Spring Boot 3.4.x]
        B[Java 17]
        C[Thymeleaf]
        D[Spring Web MVC]
    end
    
    subgraph "Data Layer"
        E[Spring Data JPA]
        F[Hibernate]
        G[PostgreSQL Driver]
    end
    
    subgraph "Integration Layer"
        H[Azure Storage SDK]
        I[Azure Service Bus SDK]
        J[Spring Cloud Azure]
        K[Jackson JSON]
    end
    
    subgraph "Azure Services"
        L[Azure Blob Storage]
        M[Azure Service Bus]
        N[Azure PostgreSQL]
        O[Application Insights]
        P[Managed Identity]
    end
    
    A --> E
    A --> H
    A --> I
    A --> J
    E --> G
    H --> L
    I --> M
    J --> O
    G --> N
    P --> L
    P --> M
    P --> N
    
    style L fill:#99ccff
    style M fill:#99ccff
    style N fill:#99ccff
    style O fill:#99ccff
    style P fill:#99ff99
```

---

## Architecture Patterns

### Layer Architecture

```mermaid
graph TD
    subgraph "Presentation Layer"
        UI[Thymeleaf Templates]
        REST[REST Controllers]
    end
    
    subgraph "Business Logic Layer"
        SVC[Service Layer]
        PROC[Message Processors]
    end
    
    subgraph "Data Access Layer"
        REPO[JPA Repositories]
        STORAGE[Storage Services]
    end
    
    subgraph "External Systems"
        CLOUD[Cloud Storage]
        QUEUE[Message Queue]
        DB[(Database)]
    end
    
    UI --> REST
    REST --> SVC
    PROC --> SVC
    SVC --> REPO
    SVC --> STORAGE
    STORAGE --> CLOUD
    SVC --> QUEUE
    PROC --> QUEUE
    REPO --> DB
    
    style UI fill:#e1f5ff
    style REST fill:#e1f5ff
    style SVC fill:#fff4e1
    style PROC fill:#fff4e1
    style REPO fill:#d4f1d4
    style STORAGE fill:#d4f1d4
```

---

## Key Architectural Decisions

### Multi-Module Design
- **Web Module**: Handles user interactions, file uploads, and API endpoints
- **Worker Module**: Dedicated to asynchronous image processing
- **Benefit**: Separation of concerns, independent scaling

### Profile-Based Configuration
- **dev profile**: Uses local file storage (LocalFileStorageService)
- **default profile**: Uses cloud storage (AwsS3Service / Future: AzureBlobService)
- **Benefit**: Easy development without cloud dependencies

### Asynchronous Processing
- Upload and thumbnail generation are decoupled via message queue
- User gets immediate feedback on upload
- Worker processes thumbnails in background
- **Benefit**: Better user experience, fault tolerance

### Storage Abstraction
- StorageService interface abstracts storage implementation
- Implementations: AwsS3Service, LocalFileStorageService
- **Migration Impact**: Only need to add AzureBlobStorageService implementation

---

## Migration Impact Analysis

### High Impact Components

1. **Storage Services** (Both Modules)
   - Current: `AwsS3Service` and `S3FileProcessingService`
   - Change: Replace AWS SDK with Azure Blob Storage SDK
   - Complexity: Medium - API translation required

2. **Message Queue** (Both Modules)
   - Current: RabbitMQ with Spring AMQP
   - Change: Azure Service Bus with Spring Cloud Azure
   - Complexity: Medium - Different programming model

3. **Authentication** (Both Modules)
   - Current: Static AWS credentials
   - Change: Azure Managed Identity (DefaultAzureCredential)
   - Complexity: Low - Simplified authentication model

### Medium Impact Components

1. **Configuration Classes**
   - `AwsS3Config` → `AzureBlobConfig`
   - `RabbitConfig` → `ServiceBusConfig`
   - Complexity: Low - Configuration changes only

2. **Database Connection**
   - Update connection strings for Azure PostgreSQL
   - Add SSL configuration
   - Complexity: Low - Minimal code changes

### Low Impact Components

1. **Controllers** - No changes required
2. **Models** - No changes required  
3. **Repositories** - No changes required
4. **Business Logic** - Minimal changes

---

## Deployment Architecture

### Current Deployment (Generic)

```mermaid
graph TB
    subgraph "Application Servers"
        WEB[Web Server :8080]
        WORK[Worker Server :8081]
    end
    
    subgraph "External Services"
        S3[AWS S3 Bucket]
        RMQ[RabbitMQ Server :5672]
        PG[PostgreSQL :5432]
    end
    
    WEB -->|HTTP/S| S3
    WEB -->|AMQP| RMQ
    WEB -->|JDBC| PG
    WORK -->|AMQP| RMQ
    WORK -->|HTTP/S| S3
    WORK -->|JDBC| PG
    
    style S3 fill:#ff9999
    style RMQ fill:#ff9999
```

### Target Deployment (Azure)

```mermaid
graph TB
    subgraph "Azure App Services"
        WEBAPP[Web App Service]
        WORKAPP[Worker App Service]
    end
    
    subgraph "Azure Platform Services"
        BLOB[Blob Storage Account]
        SB[Service Bus Namespace]
        PSQL[PostgreSQL Flexible Server]
        AI[Application Insights]
        MI[Managed Identity]
    end
    
    WEBAPP -->|Azure SDK| BLOB
    WEBAPP -->|Azure SDK| SB
    WEBAPP -->|JDBC SSL| PSQL
    WEBAPP -->|Auto| AI
    WORKAPP -->|Azure SDK| SB
    WORKAPP -->|Azure SDK| BLOB
    WORKAPP -->|JDBC SSL| PSQL
    WORKAPP -->|Auto| AI
    MI -.->|Auth| WEBAPP
    MI -.->|Auth| WORKAPP
    
    style BLOB fill:#99ccff
    style SB fill:#99ccff
    style PSQL fill:#99ccff
    style AI fill:#99ccff
    style MI fill:#99ff99
```

---

## Summary

### Architecture Strengths
- ✅ Clean separation of concerns with multi-module design
- ✅ Abstraction layer for storage (StorageService interface)
- ✅ Asynchronous processing for better scalability
- ✅ Profile-based configuration for different environments
- ✅ Standard Spring Boot patterns and best practices

### Migration Considerations
- 🔄 Replace AWS SDK with Azure SDK (7 story points effort)
- 🔄 Migrate RabbitMQ to Azure Service Bus (5 story points effort)
- 🔄 Implement Azure Managed Identity (3 story points effort)
- 🔄 Configure Azure PostgreSQL (1 story point effort)
- 🔄 Add Application Insights (1 story point effort)
- 🔄 Upgrade Spring Boot version (2 story points effort)

**Total Migration Effort**: 18-19 story points (~3-4 weeks)

### Cloud Readiness Score: 62/100
- Strong foundation with Spring Boot and standard patterns
- Requires moderate effort to migrate cloud dependencies
- Good abstraction layers make migration more manageable
- Security improvements needed (Managed Identity vs static credentials)
