# Modernization Plan: RabbitMQ to Azure Service Bus Migration

**Project**: Asset Manager Kit

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Spring Boot 2.7.14
- **Build Tool**: Maven 3.x
- **Database**: PostgreSQL
- **Key Dependencies**: Spring Boot AMQP, AWS SDK for S3

---

## Overview

> This migration replaces RabbitMQ messaging with Azure Service Bus across both the web and worker applications. The application currently uses RabbitMQ for asynchronous message processing between the web module (which handles file uploads) and the worker module (which processes thumbnails and file operations). The new architecture will:
>
> - Replace RabbitMQ with Azure Service Bus for reliable cloud-native messaging
> - Use managed identity for secure, credential-free authentication to Azure Service Bus
> - Maintain the existing asynchronous messaging patterns between web and worker modules
>
> The migration follows a phased approach, first upgrading the Java and Spring Boot versions to meet Azure SDK requirements, then migrating messaging infrastructure.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| Web         | RabbitMQ         | Azure Service Bus | Managed Identity | Migrate message publishing |
| Worker      | RabbitMQ         | Azure Service Bus | Managed Identity | Migrate message consumption |

---

## Code

### Task 1: Upgrade Spring Boot to 3.x

**Description**: Upgrade Spring Boot from 2.7.14 to 3.x to meet Azure SDK compatibility requirements and prepare for Azure Service Bus migration.

**Requirements**:
This upgrade is required because the current Java 11 and Spring Boot 2.7.14 versions are below the minimum requirements for Azure Service Bus SDK. This upgrade includes JDK 17, Spring Framework 6.x, and migration from JavaEE (javax.*) to Jakarta EE (jakarta.*).

**Environment Configuration**:


**App Scope**:
- web
- worker

**Skills**: 
- Skill Name: migration-spring-boot-upgrade
  - Skill Location: builtin

**Success Criteria**:
- Pass Build: Yes - Project must compile successfully after upgrade
- Generate New Unit Tests (Mock-based): No - Not required for upgrade task
- Generate New Integration Tests: No - Not required for upgrade task
- Pass Unit Tests: Yes - All existing tests must pass after upgrade
- Pass New Integration Tests: No - Not applicable
- Pass Security Compliance: No - Not explicitly required

### Task 2: Migrate from RabbitMQ to Azure Service Bus

**Description**: Migrate messaging infrastructure from RabbitMQ with AMQP to Azure Service Bus for cloud-native, managed messaging service.

**Requirements**:
Replace all RabbitMQ AMQP-based messaging with Azure Service Bus in both web and worker modules. Use managed identity for authentication. Maintain existing message formats and asynchronous processing patterns.

**Environment Configuration**:


**App Scope**:
- web
- worker

**Skills**: 
- Skill Name: migration-amqp-rabbitmq-servicebus
  - Skill Location: builtin

**Success Criteria**:
- Pass Build: Yes - Project must compile successfully after migration
- Generate New Unit Tests (Mock-based): No - Not required unless specified
- Generate New Integration Tests: No - Not required unless Azure resources provided
- Pass Unit Tests: Yes - All tests must pass with mocked Azure resources
- Pass New Integration Tests: No - Not required unless Azure resources provided
- Pass Security Compliance: No - Not explicitly required

---

## Clarifications

The following items were not explicitly requested but may be needed for a complete implementation:

1. **Azure Service Bus Namespace and Queue Configuration**
   - **Why needed**: Azure Service Bus requires a namespace and queues to be provisioned before the application can use them
   - **Options**: 
     - Provide existing Azure Service Bus namespace connection details
     - Create new Azure Service Bus resources as part of the migration
     - Defer resource provisioning to a separate deployment phase
   - **Recommendation**: If Azure Service Bus resources are not already available, the migration code will use configuration placeholders that need to be updated with actual connection details during deployment

2. **Integration Testing with Azure Service Bus**
   - **Why needed**: To verify the migration works correctly with actual Azure Service Bus
   - **Options**:
     - Provide Azure Service Bus test environment for integration testing
     - Skip integration tests and rely on unit tests with mocked services
   - **Recommendation**: Integration tests will be skipped by default unless Azure Service Bus resources are provided for testing

3. **Deployment Strategy**
   - **Why needed**: To determine if containerization or deployment automation is needed
   - **Options**:
     - Deploy to Azure Container Apps (requires containerization)
     - Deploy to Azure App Service (can use JAR deployment)
     - Manual deployment without automation
   - **Recommendation**: No deployment or containerization tasks are included unless explicitly requested
