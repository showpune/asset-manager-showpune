# Modernization Plan: Azure Migration

## Overview
Execute the plan to migrate the project from AWS to Azure services.

## Objective
Migrate the Asset Manager application from AWS infrastructure to Azure infrastructure, replacing:
- AWS S3 → Azure Blob Storage
- RabbitMQ → Azure Service Bus
- Password-based authentication → Managed Identity authentication

## Scope
- Web module (assets-manager-web)
- Worker module (assets-manager-worker)

## Migration Steps

### 1. Update Dependencies
- Add Azure SDK for Storage Blobs
- Add Azure SDK for Service Bus JMS
- Update Spring Boot dependencies as needed

### 2. Storage Migration (AWS S3 → Azure Blob Storage)
- Create AzureBlobStorageService implementation
- Update configuration to support Azure Blob Storage
- Maintain backward compatibility with local file storage for dev profile

### 3. Messaging Migration (RabbitMQ → Azure Service Bus)
- Update messaging configuration for Azure Service Bus
- Update connection properties
- Maintain message queue functionality

### 4. Authentication Migration
- Configure managed identity support
- Update connection strings to use DefaultAzureCredential
- Maintain password-based auth for local development

### 5. Configuration Updates
- Add Azure-specific properties
- Update application.properties files
- Document required environment variables

### 6. Testing & Validation
- Verify local development still works
- Test Azure integration (when credentials available)
- Update documentation

## Success Criteria
- All Azure SDK dependencies added
- Azure service implementations created
- Configuration updated for Azure services
- Documentation reflects Azure migration
- Local development remains functional
