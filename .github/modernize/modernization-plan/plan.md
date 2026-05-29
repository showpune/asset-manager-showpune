# Modernization Plan: modernization-plan

**Project**: assets-manager-parent

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven
- **Database**: PostgreSQL (via Spring Data JPA / Hibernate)
- **Key Dependencies**: Spring AMQP, AWS SDK v2 (S3), Spring Data JPA, Lombok, Jackson

---

## Overview

This migration moves the Asset Manager Kit application from AWS/on-premises services to Azure-managed services. The application currently uses AWS S3 for file storage, RabbitMQ for asynchronous image-processing messaging, and a self-managed PostgreSQL database with password-based authentication. The new architecture will:

- Replace AWS S3 object storage with Azure Blob Storage for managing uploaded assets and generated thumbnails
- Replace RabbitMQ (AMQP) messaging with Azure Service Bus for reliable, fully managed message queuing between the web and worker modules
- Migrate PostgreSQL connectivity to Azure Database for PostgreSQL using Managed Identity for secure, credential-free authentication

The migration follows a phased transform approach: each service is migrated independently, keeping the existing Spring Boot application structure intact.

---

## Migration Impact Summary

| Application             | Original Service  | New Azure Service                    | Authentication     | Comments                                      |
|-------------------------|-------------------|--------------------------------------|--------------------|-----------------------------------------------|
| assets-manager-web      | AWS S3            | Azure Blob Storage                   | Managed Identity   | File upload, listing, download, delete        |
| assets-manager-worker   | AWS S3            | Azure Blob Storage                   | Managed Identity   | Thumbnail generation read/write               |
| assets-manager-web      | RabbitMQ (AMQP)   | Azure Service Bus                    | Managed Identity   | Publish image-processing messages             |
| assets-manager-worker   | RabbitMQ (AMQP)   | Azure Service Bus                    | Managed Identity   | Consume image-processing messages             |
| assets-manager-web      | PostgreSQL        | Azure Database for PostgreSQL        | Managed Identity   | Image metadata persistence                    |
| assets-manager-worker   | PostgreSQL        | Azure Database for PostgreSQL        | Managed Identity   | Image metadata read/write during processing   |

---

## Migration Tasks

### Task 001 — Migrate AWS S3 to Azure Blob Storage

Migrate all AWS S3 storage interactions in both the `web` and `worker` modules to Azure Blob Storage. This includes file upload, list, download, delete, and thumbnail storage operations.

### Task 002 — Migrate RabbitMQ (AMQP) to Azure Service Bus

Migrate the asynchronous messaging layer from RabbitMQ (using AMQP) to Azure Service Bus in both the `web` and `worker` modules. This covers message publishing (web) and message consuming (worker) for the `image-processing` queue.

### Task 003 — Migrate PostgreSQL to Azure Database for PostgreSQL with Managed Identity

Migrate the database connectivity in both the `web` and `worker` modules from password-based PostgreSQL to Azure Database for PostgreSQL using Managed Identity for secure, credential-free authentication.

---
