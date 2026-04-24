# Modernization Plan: modernization-test

**Project**: assets-manager

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven
- **Database**: PostgreSQL (password-based authentication, local instance)
- **Key Dependencies**: Spring Data JPA, Spring Boot AMQP, AWS SDK for S3, Lombok

---

## Overview

This migration moves the assets-manager application's database connectivity from password-based PostgreSQL authentication to Azure Database for PostgreSQL with Managed Identity. The application currently connects to a local PostgreSQL instance (`jdbc:postgresql://localhost:5432/assets_manager`) using hardcoded credentials stored in `application.properties`. The new architecture will:

- Replace hardcoded PostgreSQL credentials with passwordless Managed Identity authentication against Azure Database for PostgreSQL
- Eliminate plaintext database passwords from configuration files to comply with security policies
- Ensure all database access uses Managed Identity, aligning with the policy requirement that service-to-service authentication uses Managed Identity

The migration follows a single-phase approach scoped solely to the PostgreSQL authentication migration, covering both the `assets-manager-web` and `assets-manager-worker` modules.

---

## Migration Impact Summary

| Application            | Original Service              | New Azure Service                   | Authentication   | Comments                                      |
|------------------------|-------------------------------|-------------------------------------|------------------|-----------------------------------------------|
| assets-manager-web     | PostgreSQL (password-based)   | Azure Database for PostgreSQL       | Managed Identity | Remove hardcoded credentials, enable MI auth  |
| assets-manager-worker  | PostgreSQL (password-based)   | Azure Database for PostgreSQL       | Managed Identity | Remove hardcoded credentials, enable MI auth  |

---

## Migration Tasks

### Task 001: Migrate PostgreSQL Authentication to Azure Managed Identity

Migrate both `assets-manager-web` and `assets-manager-worker` modules from password-based PostgreSQL authentication to passwordless Managed Identity authentication for Azure Database for PostgreSQL. This task removes hardcoded database credentials from all `application.properties` files and updates the datasource configuration to use Spring Cloud Azure with Managed Identity.

**Skill**: `migration-mi-postgresql`

---

## Completion Criteria

- Application compiles and builds successfully after migration
- All existing unit tests pass with mocked database dependencies
- No hardcoded credentials remain in `application.properties`, `application.yml`, environment variables, or source code
- PostgreSQL datasource configuration uses Managed Identity (passwordless) authentication for Azure Database for PostgreSQL
