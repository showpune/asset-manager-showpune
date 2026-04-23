# Modernization Plan: Migrate Asset Manager to Azure

**Project**: asset-manager-showpune

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven 3.x
- **Database**: PostgreSQL (local)
- **Key Dependencies**: Spring AMQP (RabbitMQ), AWS SDK v2 (S3), Spring Data JPA, Hibernate, Thymeleaf

---

## Overview

> This migration moves the Asset Manager application from AWS and local infrastructure to Azure-managed services. The application currently uses AWS S3 for file storage, RabbitMQ for async image processing messaging, and a local PostgreSQL database for metadata persistence. The new architecture will:
>
> - Replace AWS S3 with Azure Blob Storage for scalable, secure object storage with managed identity authentication
> - Replace RabbitMQ with Azure Service Bus for reliable, cloud-native async messaging between the web and worker modules
> - Replace local PostgreSQL with Azure Database for PostgreSQL with managed identity for credential-free, secure database access
>
> The migration follows a phased approach: each service dependency is migrated independently across both the `web` and `worker` modules, ensuring build stability after each task.

---

## Migration Impact Summary

| Application         | Original Service  | New Azure Service                    | Authentication    | Comments                              |
|---------------------|-------------------|--------------------------------------|-------------------|---------------------------------------|
| web, worker         | AWS S3            | Azure Blob Storage                   | Managed Identity  | File upload/download and storage      |
| web, worker         | RabbitMQ (AMQP)   | Azure Service Bus                    | Managed Identity  | Async image processing messaging      |
| web, worker         | PostgreSQL (local) | Azure Database for PostgreSQL        | Managed Identity  | Image metadata persistence            |

---

## Code

### Task 1: Migrate AWS S3 to Azure Blob Storage

**Description**: Replace AWS S3 usage with Azure Blob Storage for all file upload, download, and storage operations.

**Requirements**:

**Environment Configuration**:

**App Scope**:
- web
- worker

**Skills**:
  - Skill Name: migration-s3-to-azure-blob-storage
    - Skill Location: builtin

**Success Criteria**:
- Pass Build: Yes
- Generate New Unit Tests (Mock-based): No
- Generate New Integration Tests: No
- Pass Unit Tests: Yes
- Pass New Integration Tests: No
- Pass Security Compliance: No

---

### Task 2: Migrate RabbitMQ (AMQP) to Azure Service Bus

**Description**: Replace RabbitMQ with Azure Service Bus for async messaging between the web and worker modules.

**Requirements**:

**Environment Configuration**:

**App Scope**:
- web
- worker

**Skills**:
  - Skill Name: migration-amqp-rabbitmq-servicebus
    - Skill Location: builtin

**Success Criteria**:
- Pass Build: Yes
- Generate New Unit Tests (Mock-based): No
- Generate New Integration Tests: No
- Pass Unit Tests: Yes
- Pass New Integration Tests: No
- Pass Security Compliance: No

---

### Task 3: Migrate PostgreSQL to Azure Database for PostgreSQL

**Description**: Replace the local PostgreSQL database with Azure Database for PostgreSQL using managed identity for secure, credential-free authentication.

**Requirements**:

**Environment Configuration**:

**App Scope**:
- web
- worker

**Skills**:
  - Skill Name: migration-mi-postgresql-azure-sdk-public-cloud
    - Skill Location: builtin

**Success Criteria**:
- Pass Build: Yes
- Generate New Unit Tests (Mock-based): No
- Generate New Integration Tests: No
- Pass Unit Tests: Yes
- Pass New Integration Tests: No
- Pass Security Compliance: No

---

## Clarifications

The following items were not explicitly requested but may be needed for a complete implementation:

1. **Deployment Target**: No target Azure deployment environment was specified.
   - **Why needed**: To deploy the application to Azure, a target service is needed (e.g., Azure App Service, Azure Container Apps, AKS).
   - **Options**: Azure App Service (simplest), Azure Container Apps (containerized), AKS (Kubernetes)
   - **Recommendation**: Azure App Service for a straightforward lift-and-shift, or Azure Container Apps if containerization is desired.

2. **Azure Database for PostgreSQL**: The current project uses a local PostgreSQL instance with plaintext credentials.
   - **Why needed**: Migrating to Azure Database for PostgreSQL ensures full cloud-native operation with managed identity.
   - **Options**: Keep existing PostgreSQL as-is (external), migrate to Azure Database for PostgreSQL Flexible Server
   - **Recommendation**: Migrate to Azure Database for PostgreSQL Flexible Server with managed identity.
