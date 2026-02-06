# Modernization Plan: Azure Migration

## Overview
This plan outlines the migration of the Asset Manager application from AWS infrastructure to Azure services.

## Current State
- **Cloud Storage**: AWS S3 with password-based authentication (access key/secret key)
- **Message Queue**: RabbitMQ with password-based authentication
- **Database**: PostgreSQL with password-based authentication
- **Java Version**: Java 17
- **Spring Boot Version**: 3.2.1

## Target State
- **Cloud Storage**: Azure Blob Storage with managed identity authentication
- **Message Queue**: Azure Service Bus with managed identity authentication
- **Database**: Azure Database for PostgreSQL with managed identity authentication
- **Java Version**: Java 21 LTS
- **Spring Boot Version**: 3.4.2

## Migration Steps

### 1. Upgrade Framework Versions
- Upgrade Java from 17 to 21 LTS
- Upgrade Spring Boot from 3.2.1 to 3.4.2
- Update dependencies to compatible versions

### 2. Migrate Storage Layer (AWS S3 → Azure Blob Storage)
- Add Azure Blob Storage SDK dependencies
- Create Azure Blob Storage configuration with managed identity
- Implement AzureBlobStorageService following StorageService interface
- Add 'azure' Spring profile for Azure Blob Storage
- Maintain backward compatibility with existing 'dev' (local) profile

### 3. Migrate Message Queue (RabbitMQ → Azure Service Bus)
- Add Azure Service Bus JMS dependencies
- Create Service Bus configuration with managed identity
- Update message producers to work with Service Bus
- Update message consumers to work with Service Bus
- Ensure retry logic and error handling are maintained

### 4. Implement Managed Identity Authentication
- Use DefaultAzureCredential for all Azure services
- Remove password-based authentication for Azure services
- Configure RBAC roles:
  - Storage Blob Data Contributor for Blob Storage
  - Azure Service Bus Data Sender/Receiver for Service Bus

### 5. Update Worker Module
- Apply same Azure dependencies to worker module
- Create Azure configurations in worker module
- Update FileProcessingService implementations for Azure

### 6. Documentation and Testing
- Create comprehensive migration guide
- Document RBAC role requirements
- Test all migration paths
- Validate managed identity authentication
- Create execution summary

## Success Criteria
- Application runs successfully with 'azure' profile
- All storage operations work with Azure Blob Storage
- Message queue operations work with Azure Service Bus
- Managed identity authentication is functional
- All existing tests pass
- Migration documentation is complete
