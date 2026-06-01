# Modernization Plan: Asset Manager Migration to Azure

**Project**: Asset Manager Kit

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven (multi-module)
- **Database**: PostgreSQL (password-based authentication)
- **Key Dependencies**: Spring Data JPA, Spring AMQP, AWS SDK v2 (S3)

---

## Overview

This migration moves the Asset Manager application from AWS infrastructure to Azure-native services. The application currently uses AWS S3 for file/image storage, RabbitMQ for asynchronous image-processing messaging, and PostgreSQL with password-based authentication. The new architecture will:

- Replace AWS S3 with Azure Blob Storage for secure, scalable object storage with managed identity authentication
- Replace RabbitMQ (AMQP) with Azure Service Bus for reliable, cloud-native asynchronous messaging
- Replace password-based PostgreSQL authentication with Azure Managed Identity for credential-free, secure database connectivity

The migration is performed in independent transform tasks, each targeting a single service boundary across both the `web` and `worker` modules.

---

## Migration Impact Summary

| Application          | Original Service    | New Azure Service          | Authentication    | Comments                              |
|----------------------|---------------------|----------------------------|-------------------|---------------------------------------|
| assets-manager-web   | AWS S3              | Azure Blob Storage         | Managed Identity  | File upload, download, delete, list   |
| assets-manager-worker| AWS S3              | Azure Blob Storage         | Managed Identity  | Thumbnail download and upload         |
| assets-manager-web   | RabbitMQ (AMQP)     | Azure Service Bus          | Managed Identity  | Produce image-processing messages     |
| assets-manager-worker| RabbitMQ (AMQP)     | Azure Service Bus          | Managed Identity  | Consume image-processing messages     |
| assets-manager-web   | PostgreSQL (password)| Azure Database for PostgreSQL | Managed Identity | Image metadata persistence           |
| assets-manager-worker| PostgreSQL (password)| Azure Database for PostgreSQL | Managed Identity | Image metadata persistence           |

---

## Migration Tasks

### Task 001 — Migrate AWS S3 to Azure Blob Storage

Migrate both the `web` and `worker` modules from AWS S3 (using AWS SDK v2) to Azure Blob Storage. This covers all storage operations: listing, uploading, downloading, deleting files, and generating access URLs for images and thumbnails.

### Task 002 — Migrate RabbitMQ (AMQP) to Azure Service Bus

Migrate both the `web` and `worker` modules from RabbitMQ with AMQP to Azure Service Bus. This covers the `image-processing` queue producer in the web module and the `@RabbitListener` consumer in the worker module, including retry and manual acknowledgment behavior.

### Task 003 — Migrate PostgreSQL to Azure Database for PostgreSQL with Managed Identity

Migrate both the `web` and `worker` modules from password-based PostgreSQL authentication to Azure Managed Identity for connecting to Azure Database for PostgreSQL. Plaintext credentials in `application.properties` must be removed.
