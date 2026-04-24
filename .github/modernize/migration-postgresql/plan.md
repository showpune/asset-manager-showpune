# Modernization Plan: Migrate to Azure Database for PostgreSQL

**Project**: assets-manager

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven
- **Database**: PostgreSQL (password-based authentication, localhost)
- **Key Dependencies**: Spring Data JPA, Hibernate, PostgreSQL JDBC driver

---

## Overview

> This migration moves the assets-manager application's database connectivity from a
> locally-hosted PostgreSQL instance with hardcoded password credentials to Azure
> Database for PostgreSQL using Managed Identity for passwordless authentication.
> The application currently connects to a self-hosted PostgreSQL database using
> username/password credentials stored in application.properties. The new architecture
> will:
>
> - Eliminate hardcoded database credentials from configuration files by adopting
>   Azure Managed Identity for secure, passwordless database authentication
> - Leverage Azure Database for PostgreSQL as a fully managed, scalable cloud database
>   service, reducing operational overhead
> - Comply with the Acme Corp Modernization Playbook security requirements: no
>   hardcoded credentials, all secrets managed via Managed Identity or Azure Key Vault
>
> The migration follows a single-phase approach targeting the database connectivity
> layer in both the `web` and `worker` modules, replacing password-based datasource
> configuration with Managed Identity authentication for Azure Database for PostgreSQL.

---

## Migration Impact Summary

| Application           | Original Service          | New Azure Service                | Authentication    | Comments                     |
|-----------------------|---------------------------|----------------------------------|-------------------|------------------------------|
| assets-manager-web    | PostgreSQL (local/password) | Azure Database for PostgreSQL  | Managed Identity  | Migrate to Azure PostgreSQL  |
| assets-manager-worker | PostgreSQL (local/password) | Azure Database for PostgreSQL  | Managed Identity  | Migrate to Azure PostgreSQL  |

---

## Migration Tasks

### Task 1: Migrate to Azure Database for PostgreSQL with Managed Identity

Migrate both `assets-manager-web` and `assets-manager-worker` modules from
password-based PostgreSQL authentication to Azure Database for PostgreSQL using
Azure Managed Identity for passwordless, secure database access.

---

## Playbook Compliance

This plan follows the Acme Corp Modernization Playbook (v1.0):

- **Security**: Removes hardcoded credentials from `application.properties`; uses
  Managed Identity for all database authentication per policy requirements.
- **Target Runtime**: Project is already on Java 17 (LTS) and Spring Boot 3.2.1
  (Spring Boot 3.x), meeting the playbook's target framework requirements.
- **Authentication**: Service-to-data-plane authentication uses Managed Identity,
  satisfying the policy that "service-to-service authentication must use Managed
  Identity."
