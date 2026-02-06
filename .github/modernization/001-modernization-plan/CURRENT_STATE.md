# Current State Analysis

## Project: Asset Manager

### Current Technology Stack

#### Programming Language & Framework
- **Java Version**: 17
- **Spring Boot Version**: 3.2.1
- **Build Tool**: Maven

#### Cloud Infrastructure (AWS)
- **Storage**: AWS S3
  - Authentication: Access Key/Secret Key (password-based)
  - Usage: Original image storage and thumbnails
  
- **Message Queue**: RabbitMQ
  - Authentication: Username/Password
  - Usage: Communication between web and worker modules
  - Message flow: Image upload notifications

#### Database
- **PostgreSQL**
  - Authentication: Username/Password
  - Usage: Image metadata storage

#### Architecture
The application consists of two Spring Boot modules:
1. **Web Module**: Handles image uploads, viewing, and deletion
2. **Worker Module**: Processes images (thumbnail generation) via message queue

### Current Dependencies (Web Module)
```xml
- spring-boot-starter-web
- spring-boot-starter-thymeleaf
- spring-boot-starter-amqp (RabbitMQ)
- spring-boot-starter-data-jpa
- software.amazon.awssdk:s3 (version 2.25.13)
- postgresql driver
```

### Development Mode
- Supports `dev` profile for local development
- Uses local file system instead of S3
- Docker containers for RabbitMQ and PostgreSQL

### Identified Migration Requirements
1. Replace AWS S3 with Azure Blob Storage
2. Replace RabbitMQ with Azure Service Bus
3. Migrate to managed identity authentication (passwordless)
4. Upgrade to Java 21 LTS
5. Upgrade to latest Spring Boot 3.x
6. Maintain development mode compatibility

### Security Concerns
- Currently uses password-based authentication for all services
- Need to migrate to Azure managed identity for enhanced security
