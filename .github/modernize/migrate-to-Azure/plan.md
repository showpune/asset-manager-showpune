# Modernization Plan: Azure Migration for Asset Manager Kit

**Project**: Asset Manager Kit

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Spring Boot 2.7.14
- **Build Tool**: Maven 3.x (multi-module project)
- **Database**: PostgreSQL
- **Messaging**: RabbitMQ (AMQP via spring-boot-starter-amqp)
- **Storage**: AWS S3 (via AWS SDK 2.25.13)
- **Key Dependencies**: Spring Data JPA, Lombok, Thymeleaf

---

## Overview

This migration transforms the Asset Manager Kit application from an AWS-based architecture to Azure-native services. The application currently consists of two Spring Boot modules: a web application for file uploads/viewing and a worker service for thumbnail generation. The new architecture will:

- Replace AWS S3 storage with Azure Blob Storage for scalable asset storage
- Replace RabbitMQ messaging with Azure Service Bus for reliable message processing
- Migrate PostgreSQL database to Azure Database for PostgreSQL for managed database services
- Implement Managed Identity authentication across all Azure services for secure, credential-free access
- Provision infrastructure using Bicep for repeatable deployments

The migration follows a phased approach: infrastructure provisioning, storage migration, messaging migration, and database migration with each service maintaining backward compatibility during transition.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| Web & Worker| AWS S3           | Azure Blob Storage| Managed Identity| Migrate storage operations |
| Web & Worker| RabbitMQ         | Azure Service Bus | Managed Identity| Migrate messaging |
| Web & Worker| PostgreSQL       | Azure DB PostgreSQL| Managed Identity| Migrate database connection |

---

## Migration Tasks

### Phase 1: Infrastructure Provisioning
Generate and provision Azure infrastructure resources using Bicep templates for:
- Azure Blob Storage account for asset storage
- Azure Service Bus namespace with queues for messaging
- Azure Database for PostgreSQL flexible server
- Managed Identity configuration for all services

### Phase 2: Storage Migration
Migrate file storage operations from AWS S3 to Azure Blob Storage:
- Replace AWS SDK dependencies with Azure SDK for Storage Blobs
- Update file upload logic to use Azure Blob Storage
- Update file retrieval logic for viewing assets
- Configure Managed Identity authentication
- Ensure both web and worker modules use consistent storage access patterns

### Phase 3: Messaging Migration
Migrate message processing from RabbitMQ to Azure Service Bus:
- Replace spring-boot-starter-amqp with Azure Service Bus Spring integration
- Update message producer in web module for thumbnail generation requests
- Update message consumer in worker module for processing tasks
- Configure Managed Identity authentication
- Maintain message format compatibility during transition

### Phase 4: Database Migration
Migrate database connectivity to Azure Database for PostgreSQL:
- Update connection configuration to use Azure Database for PostgreSQL
- Configure Managed Identity authentication using Azure AD
- Maintain Spring Data JPA compatibility
- Update connection strings and authentication method

---

## Success Criteria

Each migration task will be validated against:
- **Build Success**: All modules compile without errors
- **Unit Tests**: All existing tests pass with mocked Azure resources
- **Integration Tests**: Optional tests validate Azure service connectivity
- **Security**: Managed Identity eliminates hardcoded credentials

---

## Notes

- Both web and worker modules share common dependencies and should be migrated consistently
- Managed Identity will be used throughout for secure, credential-free authentication
- Infrastructure provisioning will create resources needed for all migration phases
- Each phase can be validated independently before proceeding to the next
