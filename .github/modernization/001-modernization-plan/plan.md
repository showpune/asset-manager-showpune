# Modernization Plan: Migrate from AWS S3 to Azure Blob Storage

**Project**: asset-manager-kit

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Spring Boot 2.7.14
- **Build Tool**: Maven 3.x
- **Database**: PostgreSQL (runtime), H2 (test)
- **Key Dependencies**: AWS SDK for Java 2.25.13, Spring AMQP, Spring Data JPA, Lombok

---

## Overview

This migration aims to transition the application's object storage from AWS S3 to Azure Blob Storage. The application currently uses AWS S3 for storing and managing image files across two modules (web and worker). The new architecture will:

- Replace AWS S3 SDK with Azure Blob Storage SDK for seamless cloud storage operations
- Maintain existing storage functionality while leveraging Azure's managed storage service
- Enable integration with Azure's ecosystem for improved scalability and security

The migration follows a two-phase approach: first upgrading the Java and Spring Boot versions to meet Azure SDK requirements, then migrating the storage implementation.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| web         | AWS S3           | Azure Blob Storage| Managed Identity | Upload, list, download, delete operations |
| worker      | AWS S3           | Azure Blob Storage| Managed Identity | Thumbnail processing and storage |

---

## Code

### Task 1: Upgrade Spring Boot to 3.x

**Description**: Upgrade the application to Spring Boot 3.x to meet the requirements for Azure SDK integration and modernization.

**Requirements**: Upgrade Spring Boot from 2.7.14 to the latest 3.x version. This upgrade includes JDK 17, Spring Framework 6.x, and migration from JavaEE (javax.*) to Jakarta EE (jakarta.*).

**Environment Configuration**: None specified

**App Scope**: 
- web
- worker

**Skills**: 
- Skill Name: migration-spring-boot-upgrade
  - Skill Location: builtin

**Success Criteria**:
- Pass Build: true
- Generate New Unit Tests (Mock-based): false
- Generate New Integration Tests: false
- Pass Unit Tests: true
- Pass New Integration Tests: false
- Pass Security Compliance: false

---

### Task 2: Migrate from AWS S3 to Azure Blob Storage

**Description**: Migrate object storage from AWS S3 to Azure Blob Storage for both web and worker modules.

**Requirements**: Migrate all S3 storage operations (upload, download, list, delete) to Azure Blob Storage. Maintain existing functionality while replacing AWS SDK with Azure SDK.

**Environment Configuration**: None specified

**App Scope**:
- web
- worker

**Skills**: 
- Skill Name: migration-s3-to-azure-blob-storage
  - Skill Location: builtin

**Success Criteria**:
- Pass Build: true
- Generate New Unit Tests (Mock-based): false
- Generate New Integration Tests: false
- Pass Unit Tests: true
- Pass New Integration Tests: false
- Pass Security Compliance: false

---

## Clarifications

The following items were not explicitly requested but may be needed for a complete implementation:

1. **Azure Blob Storage Configuration**
   - **Why needed**: The application requires Azure Blob Storage container name and optional endpoint configuration for successful deployment
   - **Options**: 
     - Use environment variables for configuration (recommended for cloud deployments)
     - Use application properties files
   - **Recommendation**: Configure via environment variables (AZURE_STORAGE_ACCOUNT_NAME, AZURE_STORAGE_CONTAINER_NAME) to align with Azure managed identity best practices

2. **Data Migration Strategy**
   - **Why needed**: Existing data in S3 needs to be migrated to Azure Blob Storage
   - **Options**:
     - Manual migration using Azure tools (AzCopy)
     - Parallel dual-write during transition period
     - Leave existing data in S3 (if acceptable)
   - **Recommendation**: Use AzCopy for one-time bulk migration before cutover. This can be done separately from code migration.

3. **Testing with Azure Resources**
   - **Why needed**: Integration tests may require actual Azure Blob Storage resources
   - **Options**:
     - Use Azurite (Azure Storage emulator) for local testing
     - Use actual Azure Storage account with test containers
     - Mock Azure SDK clients in tests
   - **Recommendation**: Use Azurite for local development and CI/CD pipelines, with option to run against actual Azure resources for verification
