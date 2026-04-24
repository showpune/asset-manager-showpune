# Modernization Plan: Migrate to Azure Database for PostgreSQL

**Project**: assets-manager

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL (self-hosted, password-based authentication)
- **Key Dependencies**: Spring Data JPA, Hibernate, PostgreSQL JDBC Driver

---

## Overview

> This migration moves the assets-manager application's database connectivity from a
> self-hosted PostgreSQL instance with hardcoded password-based credentials to Azure
> Database for PostgreSQL using Managed Identity for passwordless authentication.
> The application currently connects to a local PostgreSQL database using
> hardcoded username and password properties in `application.properties`.
> The new architecture will:
>
> - Replace hardcoded PostgreSQL credentials with Azure Managed Identity authentication,
>   eliminating the need to manage database passwords
> - Connect both the `web` and `worker` modules to Azure Database for PostgreSQL
>   using secure, passwordless authentication
> - Comply with the organization's policy prohibiting hardcoded credentials and
>   requiring Managed Identity for service-to-service authentication
>
> The migration follows a single-phase approach: update the Spring Boot datasource
> configuration and dependencies in both modules to use Azure Managed Identity
> for Azure Database for PostgreSQL.

---

## Migration Impact Summary

```
| Application           | Original Service        | New Azure Service              | Authentication    | Comments                          |
|-----------------------|-------------------------|--------------------------------|-------------------|-----------------------------------|
| assets-manager-web    | PostgreSQL (local)      | Azure Database for PostgreSQL  | Managed Identity  | Remove hardcoded DB credentials   |
| assets-manager-worker | PostgreSQL (local)      | Azure Database for PostgreSQL  | Managed Identity  | Remove hardcoded DB credentials   |
```

---

## Migration Tasks

### Task 001 — Migrate to Azure Database for PostgreSQL with Managed Identity

Migrate both the `web` and `worker` modules from local PostgreSQL with
password-based authentication to Azure Database for PostgreSQL using
Azure Managed Identity for passwordless, credential-free database access.
Update Spring Cloud Azure dependencies and datasource configuration in both
modules to remove hardcoded credentials and enable Managed Identity
authentication.

---

## Playbook Policy Compliance

This plan adheres to the following policies from the Acme Corp Modernization Playbook:

- **Authentication**: Managed Identity used for database connectivity (no passwords).
- **Secrets Management**: Hardcoded credentials removed; no sensitive values remain
  in `application.properties` or source code.
- **Java & Spring Boot**: Already on Java 17 (LTS) and Spring Boot 3.2.1 (3.x) —
  both meet the target versions defined in `targets.md`.
