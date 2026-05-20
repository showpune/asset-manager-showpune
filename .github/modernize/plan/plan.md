# Modernization Plan: Asset Manager modernization to Azure

**Project**: asset-manager-showpune

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven (multi-module)
- **Database**: PostgreSQL
- **Key Dependencies**: Spring AMQP, AWS SDK S3, Spring Data JPA

---

## Overview

> This migration modernizes the Asset Manager project from
> AWS S3, RabbitMQ, and password-based PostgreSQL integration
> to Azure-managed services.
>
> - Move object storage workload to Azure Blob Storage
> - Move message queue processing to Azure Service Bus
> - Enable managed identity authentication for Azure PostgreSQL access
>
> The migration follows a phased approach with service migrations first,
> then security remediation before any deployment activities.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| Web/Worker  | AWS S3           | Azure Blob        | Managed Identity | Storage migration |
| Web/Worker  | RabbitMQ         | Azure Service Bus | Managed Identity | Messaging migration |
| Web/Worker  | PostgreSQL auth  | Azure PostgreSQL  | Managed Identity | DB auth modernization |

---

## Open Questions & Questionnaire

- [x] Q: Include infrastructure provisioning? → A: No (not requested)
- [x] Q: Include integration testing? → A: No (not explicitly requested)
- [x] Q: Include baseline setup task? → A: No (only needed with integration task)
- [x] Q: Include security/CVE task? → A: Yes (default)
- [x] Q: Include deployment task? → A: No (not explicitly requested)
- [x] Q: Include containerization task? → A: No (deployment not included)
