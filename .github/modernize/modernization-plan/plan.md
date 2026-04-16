# Modernization Plan: modernization-plan

**Project**: assets-manager

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven
- **Database**: PostgreSQL (localhost:5432/assets_manager)
- **Key Dependencies**: Spring Data JPA, Spring AMQP (RabbitMQ), AWS SDK v2 (S3), Hibernate, Lombok, Thymeleaf

---

## Overview

> This migration moves the assets-manager application from AWS and self-hosted services to Azure-native managed services. The application currently uses AWS S3 for file storage, RabbitMQ for async messaging between the web and worker services, and a PostgreSQL database for metadata persistence. The new architecture will:
>
> - Replace AWS S3 with Azure Blob Storage for scalable, cost-effective asset storage using Managed Identity authentication
> - Replace RabbitMQ with Azure Service Bus for reliable, managed message brokering without self-hosted infrastructure
> - Migrate PostgreSQL connections to Azure Database for PostgreSQL using Managed Identity for secure, credential-free authentication
> - Upgrade the Java runtime from Java 17 to Java 21 to leverage the latest LTS features and performance improvements
>
> The migration follows a phased approach: runtime upgrade first, followed by each service migration independently, and validated end-to-end with integration tests.

---

## Migration Impact Summary

| Application        | Original Service | New Azure Service              | Authentication    | Comments                              |
|--------------------|------------------|--------------------------------|-------------------|---------------------------------------|
| web, worker        | AWS S3           | Azure Blob Storage             | Managed Identity  | File upload, list, delete, thumbnail  |
| web, worker        | RabbitMQ (AMQP)  | Azure Service Bus              | Managed Identity  | image-processing queue/topic          |
| web, worker        | PostgreSQL       | Azure Database for PostgreSQL  | Managed Identity  | Shared assets_manager database        |

---

## Upgrade Tasks

### Task 001 — Upgrade Java to 21

Upgrade the Java runtime from Java 17 to Java 21 across both the `web` and `worker` modules to use the latest LTS version with improved performance and language features.

---

## Migration Tasks

### Task 002 — Migrate AWS S3 to Azure Blob Storage

Migrate file storage from AWS S3 to Azure Blob Storage in both the `web` module (uploads, listings, deletes) and the `worker` module (download originals, upload thumbnails), using Managed Identity for secure, credential-free access.

### Task 003 — Migrate RabbitMQ to Azure Service Bus

Migrate the asynchronous messaging layer from RabbitMQ (AMQP) to Azure Service Bus in both the `web` module (message publisher) and `worker` module (message consumer with retry logic), using Managed Identity for authentication.

### Task 004 — Migrate PostgreSQL to Azure Database for PostgreSQL

Migrate the database connections in both the `web` and `worker` modules from password-based PostgreSQL to Azure Database for PostgreSQL using Managed Identity for secure, credential-free authentication.

---

## Integration Test Tasks

### Task 005 — Integration Tests

Generate and run integration tests for all Azure service migrations to validate end-to-end correctness across Azure Blob Storage, Azure Service Bus, and Azure Database for PostgreSQL.

---
