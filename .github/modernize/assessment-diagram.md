# Asset Manager - Architecture Diagram

## Application Overview

The Asset Manager is a multi-module Spring Boot application designed for cloud-based image asset management with asynchronous thumbnail processing.

## Architecture Type

- **Pattern**: Microservices / Multi-Module Application
- **Communication**: Message-Driven Architecture
- **Deployment**: Distributed Services

## Technology Stack

### Current Stack
- **Framework**: Spring Boot 3.2.1
- **Language**: Java 17
- **Build Tool**: Maven
- **Storage**: AWS S3 (password-based auth)
- **Messaging**: RabbitMQ (password-based auth)
- **Database**: PostgreSQL (password-based auth)
- **UI**: Thymeleaf
- **Data Access**: Spring Data JPA / Hibernate

### Target Stack (Azure Migration)
- **Framework**: Spring Boot 3.4.x
- **Language**: Java 17
- **Build Tool**: Maven
- **Storage**: Azure Blob Storage (Managed Identity)
- **Messaging**: Azure Service Bus (Managed Identity)
- **Database**: Azure Database for PostgreSQL (Managed Identity)
- **Monitoring**: Azure Application Insights
- **UI**: Thymeleaf
- **Data Access**: Spring Data JPA / Hibernate

## Current Architecture (C4 Model - Container Level)

```mermaid
C4Context
    title Asset Manager - Current Architecture (AWS)

    Person(user, "User", "Uploads and views images")
    
    System_Boundary(app, "Asset Manager Application") {
        Container(web, "Web Module", "Spring Boot, Thymeleaf", "Handles file uploads, viewing, and user interface")
        Container(worker, "Worker Module", "Spring Boot", "Processes images and generates thumbnails")
    }
    
    System_Ext(s3, "AWS S3", "Object Storage", "Stores original images and thumbnails")
    System_Ext(rabbitmq, "RabbitMQ", "Message Broker", "Queues image processing tasks")
    System_Ext(postgres, "PostgreSQL", "Database", "Stores image metadata")
    
    Rel(user, web, "Uploads images, Views gallery", "HTTPS")
    Rel(web, s3, "Stores and retrieves files", "AWS SDK")
    Rel(web, rabbitmq, "Sends processing messages", "AMQP")
    Rel(web, postgres, "Stores metadata", "JDBC")
    Rel(rabbitmq, worker, "Delivers messages", "AMQP")
    Rel(worker, s3, "Downloads originals, Uploads thumbnails", "AWS SDK")
    Rel(worker, postgres, "Updates metadata", "JDBC")
```

## Target Architecture (C4 Model - Container Level)

```mermaid
C4Context
    title Asset Manager - Target Architecture (Azure)

    Person(user, "User", "Uploads and views images")
    
    System_Boundary(app, "Asset Manager Application") {
        Container(web, "Web Module", "Spring Boot, Thymeleaf", "Handles file uploads, viewing, and user interface")
        Container(worker, "Worker Module", "Spring Boot", "Processes images and generates thumbnails")
    }
    
    System_Ext(blob, "Azure Blob Storage", "Object Storage", "Stores original images and thumbnails")
    System_Ext(servicebus, "Azure Service Bus", "Message Broker", "Queues image processing tasks")
    System_Ext(azpostgres, "Azure PostgreSQL", "Database", "Stores image metadata")
    System_Ext(insights, "Application Insights", "Monitoring", "Distributed tracing and telemetry")
    System_Ext(identity, "Managed Identity", "Authentication", "Passwordless authentication")
    
    Rel(user, web, "Uploads images, Views gallery", "HTTPS")
    Rel(web, blob, "Stores and retrieves files", "Azure SDK")
    Rel(web, servicebus, "Sends processing messages", "Azure SDK")
    Rel(web, azpostgres, "Stores metadata", "JDBC with SSL")
    Rel(web, insights, "Sends telemetry", "Azure SDK")
    Rel(servicebus, worker, "Delivers messages", "Azure SDK")
    Rel(worker, blob, "Downloads originals, Uploads thumbnails", "Azure SDK")
    Rel(worker, azpostgres, "Updates metadata", "JDBC with SSL")
    Rel(worker, insights, "Sends telemetry", "Azure SDK")
    Rel(web, identity, "Authenticates", "DefaultAzureCredential")
    Rel(worker, identity, "Authenticates", "DefaultAzureCredential")
```

## Application Layers

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[Thymeleaf Templates]
        REST[REST Controllers]
    end
    
    subgraph "Business Layer"
        WebService[Storage Service]
        WorkerService[File Processing Service]
        MessageService[Message Processor]
    end
    
    subgraph "Data Access Layer"
        JPA[Spring Data JPA]
        Repo[Image Metadata Repository]
    end
    
    subgraph "Infrastructure Layer - Current"
        S3[AWS S3 Client]
        Rabbit[RabbitMQ Template]
        DB1[PostgreSQL Driver]
    end
    
    subgraph "Infrastructure Layer - Target"
        Blob[Blob Service Client]
        ServiceBus[Service Bus Client]
        DB2[PostgreSQL Driver with SSL]
        MI[Managed Identity]
        AI[Application Insights]
    end
    
    UI --> REST
    REST --> WebService
    WebService --> JPA
    WebService --> S3
    WebService --> Rabbit
    MessageService --> WorkerService
    WorkerService --> JPA
    WorkerService --> S3
    JPA --> Repo
    Repo --> DB1
    
    style S3 fill:#ff9999
    style Rabbit fill:#ff9999
    style DB1 fill:#ffcc99
    style Blob fill:#99ff99
    style ServiceBus fill:#99ff99
    style DB2 fill:#99ff99
    style MI fill:#99ccff
    style AI fill:#99ccff
```

## Data Flow - Image Upload and Processing

```mermaid
sequenceDiagram
    actor User
    participant Web as Web Module
    participant Storage as Cloud Storage
    participant Queue as Message Queue
    participant Worker as Worker Module
    participant DB as Database

    User->>Web: Upload image file
    Web->>Storage: Store original image
    Storage-->>Web: Confirm storage
    Web->>DB: Save image metadata
    DB-->>Web: Confirm save
    Web->>Queue: Send processing message
    Queue-->>Web: Acknowledge
    Web-->>User: Upload complete
    
    Queue->>Worker: Deliver message
    Worker->>Storage: Download original image
    Storage-->>Worker: Return image data
    Worker->>Worker: Generate thumbnail
    Worker->>Storage: Upload thumbnail
    Storage-->>Worker: Confirm upload
    Worker->>DB: Update metadata with thumbnail info
    DB-->>Worker: Confirm update
    Worker->>Queue: Acknowledge message
```

## Module Structure

```mermaid
graph LR
    Parent[assets-manager-parent<br/>Parent POM<br/>Spring Boot 3.2.1]
    
    Web[Web Module<br/>assets-manager-web<br/>Port: 8080]
    Worker[Worker Module<br/>assets-manager-worker<br/>Port: 8081]
    
    Parent --> Web
    Parent --> Worker
    
    subgraph "Web Dependencies"
        WebThyme[Thymeleaf]
        WebSpring[Spring Web]
        WebAMQP[Spring AMQP]
        WebS3[AWS S3 SDK]
        WebJPA[Spring Data JPA]
    end
    
    subgraph "Worker Dependencies"
        WorkerSpring[Spring Core]
        WorkerAMQP[Spring AMQP]
        WorkerS3[AWS S3 SDK]
        WorkerJPA[Spring Data JPA]
        WorkerJackson[Jackson]
    end
    
    Web -.-> WebThyme
    Web -.-> WebSpring
    Web -.-> WebAMQP
    Web -.-> WebS3
    Web -.-> WebJPA
    
    Worker -.-> WorkerSpring
    Worker -.-> WorkerAMQP
    Worker -.-> WorkerS3
    Worker -.-> WorkerJPA
    Worker -.-> WorkerJackson
    
    style WebAMQP fill:#ff9999
    style WebS3 fill:#ff9999
    style WorkerAMQP fill:#ff9999
    style WorkerS3 fill:#ff9999
```

## Configuration Profiles

```mermaid
graph TB
    App[Asset Manager Application]
    
    subgraph "Development Profile"
        DevStorage[Local File System]
        DevQueue[Docker RabbitMQ]
        DevDB[Docker PostgreSQL]
    end
    
    subgraph "Default Profile - Current"
        ProdStorage[AWS S3]
        ProdQueue[RabbitMQ Server]
        ProdDB[PostgreSQL Server]
    end
    
    subgraph "Production Profile - Target"
        AzStorage[Azure Blob Storage]
        AzQueue[Azure Service Bus]
        AzDB[Azure PostgreSQL]
        AzMI[Managed Identity]
        AzAI[Application Insights]
    end
    
    App -->|dev profile| DevStorage
    App -->|dev profile| DevQueue
    App -->|dev profile| DevDB
    
    App -->|default profile| ProdStorage
    App -->|default profile| ProdQueue
    App -->|default profile| ProdDB
    
    App -->|azure profile| AzStorage
    App -->|azure profile| AzQueue
    App -->|azure profile| AzDB
    App -->|azure profile| AzMI
    App -->|azure profile| AzAI
    
    style ProdStorage fill:#ff9999
    style ProdQueue fill:#ff9999
    style AzStorage fill:#99ff99
    style AzQueue fill:#99ff99
    style AzDB fill:#99ff99
    style AzMI fill:#99ccff
    style AzAI fill:#99ccff
```

## Security Model Comparison

```mermaid
graph LR
    subgraph "Current - Credential-Based"
        App1[Application]
        Creds1[Static Credentials<br/>Access Keys<br/>Passwords]
        S3Curr[AWS S3]
        RabbitCurr[RabbitMQ]
        DBCurr[PostgreSQL]
        
        App1 --> Creds1
        Creds1 -.->|username/password| S3Curr
        Creds1 -.->|username/password| RabbitCurr
        Creds1 -.->|username/password| DBCurr
    end
    
    subgraph "Target - Identity-Based"
        App2[Application]
        MI[Managed Identity<br/>No Credentials<br/>Azure AD]
        BlobTgt[Azure Blob Storage]
        SBTgt[Azure Service Bus]
        DBTgt[Azure PostgreSQL]
        
        App2 --> MI
        MI ==>|token-based| BlobTgt
        MI ==>|token-based| SBTgt
        MI ==>|token-based| DBTgt
    end
    
    style Creds1 fill:#ff9999
    style S3Curr fill:#ffcccc
    style RabbitCurr fill:#ffcccc
    style DBCurr fill:#ffcccc
    style MI fill:#99ff99
    style BlobTgt fill:#ccffcc
    style SBTgt fill:#ccffcc
    style DBTgt fill:#ccffcc
```

## Migration Impact Analysis

### High-Impact Changes

1. **Storage Layer** (7 story points)
   - Replace AWS S3 SDK with Azure Blob Storage SDK
   - Update all storage operations in both modules
   - Change configuration from bucket to container
   - Migrate authentication from access keys to Managed Identity

2. **Messaging Layer** (5 story points)
   - Replace RabbitMQ with Azure Service Bus
   - Update message sending and receiving logic
   - Convert queue configuration
   - Migrate authentication from username/password to Managed Identity

3. **Authentication** (3 story points)
   - Remove all hardcoded credentials
   - Implement DefaultAzureCredential
   - Update configuration files
   - Remove sensitive data from properties

### Medium-Impact Changes

4. **Database Configuration** (1 story point)
   - Update connection strings for Azure PostgreSQL
   - Enable SSL/TLS connections
   - Optional: Implement Managed Identity for database

5. **Monitoring** (2 story points)
   - Add Application Insights dependency
   - Configure distributed tracing
   - Add health check endpoints
   - Upgrade Spring Boot version

## Key Architecture Patterns

### Current Patterns
- **Multi-Module Maven Project**: Separation of concerns between web and worker
- **Message-Driven Architecture**: Asynchronous processing via message queue
- **Repository Pattern**: Spring Data JPA for data access
- **Profile-Based Configuration**: Different configs for dev vs production
- **Static Credential Authentication**: Username/password for all services

### Target Patterns (Post-Migration)
- **Multi-Module Maven Project**: Maintained
- **Message-Driven Architecture**: Maintained with Azure Service Bus
- **Repository Pattern**: Maintained
- **Profile-Based Configuration**: Enhanced with Azure-specific profiles
- **Managed Identity Authentication**: Passwordless, token-based auth
- **Distributed Tracing**: Application Insights integration

## Dependencies Summary

### Critical Dependencies (Require Migration)
- `software.amazon.awssdk:s3` → `com.azure:azure-storage-blob`
- `spring-boot-starter-amqp` → `spring-cloud-azure-starter-servicebus`

### Additional Dependencies (Recommended)
- `spring-cloud-azure-dependencies` (BOM for version management)
- `spring-cloud-azure-starter-monitor` (Application Insights)
- `spring-boot-starter-actuator` (Health checks)

### Unchanged Dependencies
- Spring Boot core dependencies
- Spring Data JPA
- PostgreSQL driver (with SSL configuration)
- Thymeleaf
- Lombok
- Jackson

## Cloud Readiness Assessment

### Current State
- **Cloud Readiness Score**: 62/100
- **Total Incidents**: 34
- **Critical Issues**: 7
- **Estimated Migration Effort**: 18 story points (3-4 weeks)

### Blockers
- Hardcoded credentials in configuration
- AWS-specific SDK usage throughout codebase
- RabbitMQ coupling in messaging layer
- No cloud-native monitoring

### Enablers
- Modern Spring Boot framework (3.2.1)
- Clean separation of concerns
- Profile-based configuration
- Standard JPA for database access
- No proprietary AWS features used

## Notes

- Both modules share similar migration patterns
- Storage and messaging changes are independent and can be parallelized
- Development profile (local file system) remains unchanged
- Database migration has minimal code impact
- No application logic changes required
- UI/UX remains completely unchanged
