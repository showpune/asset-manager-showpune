# Modernization Plan: modernization-plan

**Project**: asset-manager-showpune

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven 3.x (multi-module: web, worker)
- **Database**: PostgreSQL (password-based authentication)
- **Key Dependencies**: Spring Data JPA, Spring AMQP (RabbitMQ), AWS SDK v2 (S3), Lombok, SLF4J (Logback)

---

## Overview

> This migration modernizes the asset-manager-showpune application from AWS infrastructure to Azure services. The application currently uses AWS S3 for image storage, RabbitMQ for message queuing, PostgreSQL with password-based credentials, SLF4J logging, and hardcoded credentials in configuration files. The new architecture will:
>
> - Replace AWS S3 with Azure Blob Storage using Managed Identity for credential-free, secure storage access
> - Replace RabbitMQ with Azure Service Bus using Managed Identity for secure, managed message queuing
> - Upgrade PostgreSQL connection to Azure Database for PostgreSQL with Managed Identity authentication
> - Externalize all remaining plaintext credentials and secrets to Azure Key Vault
> - Migrate SLF4J logging to InternalLogger for trace context propagation and structured JSON output
> - Containerize both web and worker modules for deployment to Azure Kubernetes Service (AKS)
>
> The migration follows a phased approach: first migrating core Azure services (storage, messaging, database) in parallel, then consolidating secrets management with Key Vault, migrating the logging framework, and finally containerizing the application for AKS deployment.

---

## Migration Impact Summary

| Application  | Original Service         | New Azure Service                    | Authentication    | Comments                                    |
|--------------|--------------------------|--------------------------------------|-------------------|---------------------------------------------|
| web, worker  | AWS S3                   | Azure Blob Storage                   | Managed Identity  | Replace AWS SDK S3Client with Azure Blob SDK |
| web, worker  | RabbitMQ (AMQP)          | Azure Service Bus                    | Managed Identity  | Replace Spring AMQP with Azure Service Bus  |
| web, worker  | PostgreSQL               | Azure Database for PostgreSQL        | Managed Identity  | Passwordless DB auth via Managed Identity   |
| web, worker  | Hardcoded credentials    | Azure Key Vault                      | Managed Identity  | All secrets in application.properties       |
| web, worker  | SLF4J (@Slf4j)           | InternalLogger                       | N/A               | com.acme.logging.InternalLogger             |
| web, worker  | JAR files                | Docker containers (AKS)              | N/A               | mcr.microsoft.com/openjdk/jdk:17-distroless |

---

## Migration Tasks

See `tasks.json` for the detailed task breakdown and execution plan.

### Task Overview

1. **001-transform-s3-to-azure-blob-storage**: Migrate AWS S3 to Azure Blob Storage with Managed Identity
2. **002-transform-rabbitmq-to-servicebus**: Migrate RabbitMQ to Azure Service Bus with Managed Identity
3. **003-transform-postgresql-to-azure-postgresql**: Migrate PostgreSQL to Azure Database for PostgreSQL with Managed Identity
4. **004-transform-plaintext-credentials-to-keyvault**: Migrate plaintext credentials to Azure Key Vault _(depends on 001, 002, 003)_
5. **005-transform-logging-to-internallogger**: Migrate SLF4J logging to InternalLogger
6. **006-containerization**: Containerize web and worker modules for AKS deployment _(depends on 001–005)_
