# Modernization Plan: The Gaming Platform — Azure Migration

**Project**: The-Gaming-Platform

---

## Technical Framework

- **Language**: Java 8 (1.8)
- **Framework**: Spring Boot 2.5.7 (root modules); Spring Boot 2.5.3 (post-service)
- **Build Tool**: Maven (multi-module)
- **Database**: PostgreSQL (authentication-service, user-service), MongoDB Atlas
  (user-service, post-service), Redis (authentication-service)
- **Messaging**: RabbitMQ with Spring AMQP (all 6 services)
- **File Storage**: Firebase Cloud Storage (user-service, post-service)
- **Key Dependencies**: Spring Data JPA, Spring Data MongoDB, Spring Data Redis,
  Spring AMQP, Spring Security, JJWT, Firebase Admin SDK,
  springfox-swagger2

---

## Overview

This migration moves The Gaming Platform (a multi-service Java application) from
on-premises and third-party services to Azure-managed equivalents. The application
currently uses RabbitMQ for inter-service messaging, a local Redis instance for JWT
token caching, MongoDB Atlas for document storage, PostgreSQL for relational data,
Firebase Storage for file uploads, and custom JWT-based authentication with plaintext
credentials scattered across configuration files.

The new architecture will:

- Replace RabbitMQ with Azure Service Bus for fully managed, scalable messaging
  across all six microservices
- Replace the local Redis instance with Azure Managed Redis for secure,
  high-availability token caching
- Migrate file storage to mounted Azure Blob Storage for durable, cost-effective
  object storage
- Migrate MongoDB Atlas to Azure Cosmos DB for MongoDB secured with Managed
  Identity via Azure SDK (public cloud)
- Secure PostgreSQL connections using Azure Managed Identity, eliminating
  password-based database authentication
- Replace custom JWT authentication with Microsoft Entra ID for
  enterprise-grade identity management
- Migrate all plaintext credentials to Azure Key Vault for centralized,
  auditable secret management
- Upgrade the entire platform to Spring Boot 3.x (Java 21) as the foundation
  for all Azure integrations

The migration follows a phased approach: runtime upgrade first, then service
integrations, then identity and secrets hardening.

---

## Migration Impact Summary

| Application            | Original Service    | New Azure Service            | Authentication     | Comments                        |
|------------------------|---------------------|------------------------------|--------------------|---------------------------------|
| All Services           | RabbitMQ            | Azure Service Bus            | Managed Identity   | Spring AMQP migration           |
| authentication-service | Redis (local)       | Azure Managed Redis          | Managed Identity   | JWT token blacklist/cache       |
| user-service           | Firebase Storage    | Azure Blob Storage (mounted) | Managed Identity   | Profile image uploads           |
| post-service           | Firebase Storage    | Azure Blob Storage (mounted) | Managed Identity   | Post image attachments          |
| user-service           | MongoDB Atlas       | Azure Cosmos DB for MongoDB  | Service Connector  | userPostInteraction DB          |
| post-service           | MongoDB Atlas       | Azure Cosmos DB for MongoDB  | Service Connector  | PostMS DB                       |
| authentication-service | PostgreSQL          | Azure Database for PostgreSQL| Managed Identity   | User accounts / JPA             |
| user-service           | PostgreSQL          | Azure Database for PostgreSQL| Managed Identity   | User data / JPA                 |
| authentication-service | Custom JWT Auth     | Microsoft Entra ID           | OAuth2/OIDC        | Login, logout, token verify     |
| All Services           | Plaintext credentials| Azure Key Vault             | Managed Identity   | DB passwords, RabbitMQ creds,   |
|                        |                     |                              |                    | JWT secret, Firebase keys       |

---

## Migration Tasks

### Task 1 — Upgrade to Spring Boot 3.x
Upgrade the entire platform from Spring Boot 2.5.x (Java 8) to Spring Boot 3.x
(Java 21). This is a prerequisite for all subsequent Azure service integrations.

### Task 2 — Migrate RabbitMQ to Azure Service Bus (Spring AMQP)
Replace the RabbitMQ broker used across all six microservices with Azure Service Bus,
retaining the Spring AMQP programming model.

### Task 3 — Migrate Redis Cache to Azure Managed Redis
Replace the local Redis instance in `authentication-service` with Azure Managed Redis
for JWT token blacklist storage.

### Task 4 — Migrate File Storage to Mounted Azure Blob Storage
Replace the current file storage solution in `user-service` and `post-service` with
mounted Azure Blob Storage paths.

### Task 5 — Secure Azure Cosmos DB for MongoDB with Service Connector (Azure SDK)
Replace the MongoDB Atlas connections in `user-service` and `post-service` with Azure
Cosmos DB for MongoDB, secured using Service Connector via Azure SDK for public cloud.

### Task 6 — Secure Azure Database for PostgreSQL with Managed Identity
Replace password-based PostgreSQL connections in `authentication-service` and
`user-service` with Managed Identity authentication against Azure Database for
PostgreSQL.

### Task 7 — Migrate Authentication to Microsoft Entra ID
Replace the custom JWT-based authentication in `authentication-service` with Microsoft
Entra ID for enterprise-grade identity management.

### Task 8 — Migrate Plaintext Credentials to Azure Key Vault
Move all plaintext secrets (database passwords, RabbitMQ credentials, JWT secret,
Firebase keys) from configuration files across all services to Azure Key Vault.
