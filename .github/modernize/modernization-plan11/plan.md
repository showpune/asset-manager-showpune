# Modernization Plan: modernization-plan11

**Project**: Asset Manager Kit

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven 3.6+
- **Database**: PostgreSQL (localhost:5432, assets_manager database)
- **Key Dependencies**: Spring Data JPA, Spring AMQP, AWS SDK v2 (S3), PostgreSQL JDBC driver, Lombok

---

## Overview

This migration transforms the Asset Manager Kit application from AWS and self-hosted services to Azure-native managed services. The application currently uses AWS S3 for file/image storage, RabbitMQ (AMQP) for image-processing message queuing, and a local PostgreSQL database for metadata persistence. The new architecture will:

- Replace AWS S3 with Azure Blob Storage for secure, scalable cloud-native file storage with managed identity authentication
- Replace RabbitMQ with Azure Service Bus for reliable, managed cloud messaging without credential management overhead
- Replace the local PostgreSQL connection with Azure Database for PostgreSQL using managed identity for credential-free, secure database access

The migration follows a transform-only approach: code changes are applied to both the `web` and `worker` modules to connect to Azure services, using managed identity wherever possible.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|-----------------|-------------------|----------------|----------|
| web, worker | AWS S3 | Azure Blob Storage | Managed Identity | Migrate S3 upload/download/list/delete operations |
| web, worker | RabbitMQ (AMQP) | Azure Service Bus | Managed Identity | Migrate image-processing queue producer and consumer |
| web, worker | PostgreSQL (local) | Azure Database for PostgreSQL | Managed Identity | Migrate datasource connection with credential-free auth |

---

## Migration Tasks

### Task 001 — Migrate AWS S3 to Azure Blob Storage

Migrate all file storage operations in the `web` and `worker` modules from AWS S3 to Azure Blob Storage. This includes uploading originals and thumbnails, downloading files, listing assets, generating access URLs, and deleting files. The `StorageService` abstraction and both `AwsS3Service` and profile-based configuration will be replaced with Azure Blob Storage client using managed identity authentication.

**Skill**: `migration-s3-to-azure-blob-storage` (project)

---

### Task 002 — Migrate RabbitMQ to Azure Service Bus

Migrate the AMQP-based messaging in both `web` and `worker` modules from RabbitMQ to Azure Service Bus. This includes the `image-processing` queue producer (web module) and the `@RabbitListener` consumer (worker module), along with retry logic and message serialization configuration. Authentication will use managed identity.

**Skill**: `migration-amqp-rabbitmq-servicebus` (project)

---

### Task 003 — Migrate PostgreSQL to Azure Database for PostgreSQL

Migrate the database connection configuration in both `web` and `worker` modules from password-based local PostgreSQL to Azure Database for PostgreSQL using managed identity for secure, credential-free authentication. The JPA/Hibernate entities and repository interfaces remain unchanged; only the connection and authentication configuration is updated.

**Skill**: `migration-mi-postgresql-azure-sdk-public-cloud` (project)

---
