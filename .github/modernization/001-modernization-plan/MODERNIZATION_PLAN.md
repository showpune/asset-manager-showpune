# Modernization Plan: Azure Migration

## Overview
This plan outlines the migration from AWS-based infrastructure to Azure cloud services, along with modernization of the Java/Spring Boot stack.

## Migration Goals
1. **Cloud Platform Migration**: AWS → Azure
2. **Language & Framework Modernization**: Java 17 → Java 21 LTS, Spring Boot 3.2.1 → 3.4.2
3. **Security Enhancement**: Password-based auth → Managed Identity (passwordless)
4. **Messaging System Migration**: RabbitMQ → Azure Service Bus

## Detailed Migration Plan

### Phase 1: Java and Spring Boot Upgrade
**Goal**: Upgrade to Java 21 LTS and Spring Boot 3.4.2

**Steps**:
1. Update parent pom.xml:
   - Change Java version from 17 to 21
   - Update Spring Boot version from 3.2.1 to 3.4.2
2. Update web and worker module pom.xml files as needed
3. Verify all dependencies are compatible with Java 21 and Spring Boot 3.4.2
4. Build and test the application

**Expected Changes**:
- `pom.xml`: Java version property
- `pom.xml`: Spring Boot parent version

### Phase 2: Azure Blob Storage Integration
**Goal**: Add Azure Blob Storage as an alternative to AWS S3

**Steps**:
1. Add Azure Blob Storage dependency to web module:
   - `com.azure:azure-storage-blob:12.29.0`
   - `com.azure:azure-identity:1.14.2` (for managed identity)
2. Create `AzureBlobConfig` configuration class
3. Create `AzureBlobStorageService` implementing `StorageService` interface
4. Use `DefaultAzureCredential` for managed identity authentication
5. Add Azure-specific properties in application.properties
6. Create Spring profile `azure` to activate Azure Blob Storage

**Expected Changes**:
- `web/pom.xml`: Add Azure dependencies
- `web/src/main/java/com/microsoft/migration/assets/config/AzureBlobConfig.java`: New file
- `web/src/main/java/com/microsoft/migration/assets/service/AzureBlobStorageService.java`: New file
- `web/src/main/resources/application.properties`: Add Azure configuration

### Phase 3: Azure Service Bus Integration
**Goal**: Add Azure Service Bus as an alternative to RabbitMQ

**Steps**:
1. Add Azure Service Bus JMS dependency to both web and worker modules:
   - `com.azure.spring:spring-cloud-azure-starter-servicebus-jms:5.18.0`
2. Create `ServiceBusConfig` configuration class in both modules
3. Configure JMS connection factory for Azure Service Bus
4. Use managed identity authentication
5. Update message listeners to work with Service Bus
6. Add Service Bus properties in application.properties

**Expected Changes**:
- `web/pom.xml` and `worker/pom.xml`: Add Azure Service Bus dependencies
- `web/src/main/java/com/microsoft/migration/assets/config/ServiceBusConfig.java`: New file
- `worker/src/main/java/com/microsoft/migration/assets/worker/config/ServiceBusConfig.java`: New file
- `web/src/main/resources/application.properties`: Add Service Bus configuration
- `worker/src/main/resources/application.properties`: Add Service Bus configuration

### Phase 4: Testing and Validation
**Goal**: Ensure the migrated application works correctly

**Steps**:
1. Run unit tests
2. Verify Azure profile activation works correctly
3. Test managed identity configuration
4. Validate backward compatibility with dev profile

### Phase 5: Documentation
**Goal**: Document the migration process and deployment requirements

**Steps**:
1. Create MIGRATION_GUIDE.md with deployment instructions
2. Create EXECUTION_SUMMARY.md documenting what was done
3. Update README.md with Azure architecture information

## Azure Services Configuration Requirements

### Azure Blob Storage
- **Authentication**: Managed Identity (DefaultAzureCredential)
- **Required RBAC Role**: Storage Blob Data Contributor
- **Configuration**:
  - Storage account endpoint URL
  - Container name

### Azure Service Bus
- **Authentication**: Managed Identity (DefaultAzureCredential)
- **Required RBAC Roles**: 
  - Azure Service Bus Data Sender
  - Azure Service Bus Data Receiver
- **Configuration**:
  - Service Bus namespace hostname
  - Queue name

## Risk Mitigation
- Keep existing AWS/RabbitMQ code intact
- Use Spring profiles to switch between implementations
- Maintain dev profile for local development
- No breaking changes to existing functionality

## Success Criteria
- [ ] Application builds successfully with Java 21 and Spring Boot 3.4.2
- [ ] Azure Blob Storage service implemented and working with `azure` profile
- [ ] Azure Service Bus integration working with `azure` profile
- [ ] All existing tests pass
- [ ] Dev profile still works with local file storage
- [ ] Managed identity authentication properly configured
- [ ] Documentation complete
