# Modernization Plan: Migrate to Azure Database for PostgreSQL

**Project**: Asset Manager (assets-manager-web, assets-manager-worker)

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven (multi-module: web, worker)
- **Database**: PostgreSQL (local, password-based authentication)
- **Key Dependencies**: Spring Data JPA, Spring AMQP (RabbitMQ), AWS S3 SDK

---

## Overview

> This migration moves the Asset Manager application's database connectivity from
> a locally hosted PostgreSQL instance using password-based authentication to
> Azure Database for PostgreSQL using Managed Identity for secure,
> credential-free access. The application currently stores database credentials
> (username/password) directly in `application.properties` for both the web and
> worker modules, which violates the organization's security policies. The new
> architecture will:
>
> - Eliminate hardcoded database credentials by adopting Managed Identity for
>   passwordless authentication to Azure Database for PostgreSQL, satisfying
>   the policy requirement that sensitive credentials must not be stored in
>   source code or configuration files.
> - Provide a fully managed, scalable, and highly available PostgreSQL service
>   on Azure, reducing operational overhead for database administration.
> - Align with the Acme Corp Modernization Playbook requirements for Managed
>   Identity-based service-to-service authentication and secure secrets
>   management.
>
> The migration follows a single-phase approach: update both the `web` and
> `worker` modules to connect to Azure Database for PostgreSQL using Managed
> Identity instead of password credentials.

---

## Migration Impact Summary

| Application            | Original Service        | New Azure Service                    | Authentication   | Comments                              |
|------------------------|-------------------------|--------------------------------------|------------------|---------------------------------------|
| assets-manager-web     | Local PostgreSQL        | Azure Database for PostgreSQL        | Managed Identity | Migrate password auth to MI           |
| assets-manager-worker  | Local PostgreSQL        | Azure Database for PostgreSQL        | Managed Identity | Migrate password auth to MI           |

---

## Migration Tasks

### Task 001 — Migrate PostgreSQL to Azure Database for PostgreSQL

**Goal**: Migrate both the `assets-manager-web` and `assets-manager-worker`
modules from connecting to a locally hosted PostgreSQL instance with
password-based authentication to Azure Database for PostgreSQL using
Managed Identity for secure, credential-free database access.

**Scope**:
- Both modules (`web` and `worker`) use Spring Data JPA with the PostgreSQL
  JDBC driver and store credentials in `application.properties`.
- Update both modules to authenticate to Azure Database for PostgreSQL via
  Managed Identity, removing all hardcoded credentials.
- Ensure the application compiles and unit tests pass after the migration.
