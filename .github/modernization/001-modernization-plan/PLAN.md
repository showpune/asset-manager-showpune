# Modernization Plan: Migrate to Azure

## Overview
This modernization plan details the migration of the Asset Manager application from AWS services to Azure services, implementing managed identity authentication for improved security.

## Current State
- **Storage**: AWS S3 with access key/secret key authentication
- **Message Queue**: RabbitMQ with username/password authentication  
- **Database**: PostgreSQL with username/password authentication
- **Java Version**: Java 17
- **Spring Boot Version**: 3.2.1

## Target State
- **Storage**: Azure Blob Storage with managed identity authentication
- **Message Queue**: Azure Service Bus with managed identity authentication
- **Database**: Azure Database for PostgreSQL with managed identity authentication
- **Java Version**: Java 21 LTS (upgrade as part of modernization)
- **Spring Boot Version**: 3.4.x (latest stable)

## Migration Steps

### Phase 1: Infrastructure Setup
1. Create Azure Blob Storage container
2. Create Azure Service Bus namespace and queue
3. Set up managed identities for applications
4. Configure Azure Database for PostgreSQL

### Phase 2: Code Changes

#### 2.1 Upgrade Java and Spring Boot
- Upgrade from Java 17 to Java 21 LTS
- Upgrade Spring Boot from 3.2.1 to latest 3.4.x
- Update all dependencies to compatible versions
- Use OpenRewrite for automated migration

#### 2.2 Azure Blob Storage Implementation
- Add Azure Blob Storage SDK dependencies
- Create `AzureBlobConfig` configuration class
- Implement `AzureBlobStorageService` implementing `StorageService` interface
- Use `DefaultAzureCredential` for managed identity authentication
- Add 'azure' Spring profile for Azure Blob Storage activation
- Update web module configuration

#### 2.3 Azure Service Bus Implementation
- Add Azure Service Bus JMS dependencies
- Create `ServiceBusConfig` configuration class for web and worker modules
- Configure connection factory using managed identity
- Replace RabbitMQ listeners and senders with Service Bus equivalents
- Maintain message structure compatibility

#### 2.4 Testing Support
- Ensure local development profile still works with local file system
- Add configuration for testing Azure services locally (Azurite for storage)

### Phase 3: Testing & Validation
1. Unit tests for new Azure services
2. Integration tests with local Azure emulators
3. Build verification
4. Documentation updates

### Phase 4: Documentation
1. Update README with Azure deployment instructions
2. Create migration guide
3. Document managed identity setup
4. Create execution summary

## Success Criteria
- ✅ All tests pass
- ✅ Application builds successfully
- ✅ Azure Blob Storage service implemented with managed identity
- ✅ Azure Service Bus messaging implemented with managed identity
- ✅ Local dev profile still functional
- ✅ Documentation updated

## Risk Mitigation
- Maintain backward compatibility with existing AWS/RabbitMQ code
- Use Spring profiles to allow easy switching between cloud providers
- Comprehensive testing before production deployment
