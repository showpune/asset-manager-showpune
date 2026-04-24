# Modernization Plan: Migrate to Azure Database for PostgreSQL

**Project**: asset-manager

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven 3.x
- **Database**: PostgreSQL (password-based, local instance)
- **Key Dependencies**: Spring Data JPA, Hibernate, PostgreSQL JDBC driver

---

## Overview

> This migration moves the asset-manager application's database layer from a locally-hosted
> PostgreSQL instance using password-based authentication to Azure Database for PostgreSQL
> with passwordless Managed Identity authentication. The application currently stores
> database credentials (username/password) directly in `application.properties` for both
> the `web` and `worker` modules. The new architecture will:
>
> - Replace hardcoded PostgreSQL credentials with Azure Managed Identity for secure,
>   passwordless database connectivity in both modules
> - Eliminate credential exposure in configuration files, aligning with the organization's
>   policy of storing no secrets in source code or application properties
> - Leverage Spring Cloud Azure to provide seamless, token-based authentication against
>   Azure Database for PostgreSQL
>
> The migration updates Spring Cloud Azure dependencies and datasource configuration in
> both the `web` and `worker` modules to use Managed Identity, removing all hardcoded
> passwords from application configuration.

---

## Migration Impact Summary

| Application             | Original Service          | New Azure Service                     | Authentication    | Comments                                          |
|-------------------------|---------------------------|---------------------------------------|-------------------|---------------------------------------------------|
| assets-manager-web      | Local PostgreSQL (password) | Azure Database for PostgreSQL        | Managed Identity  | Remove hardcoded credentials from application.properties |
| assets-manager-worker   | Local PostgreSQL (password) | Azure Database for PostgreSQL        | Managed Identity  | Remove hardcoded credentials from application.properties |

---

## Migration Tasks

### Task 001 — Migrate to Azure Database for PostgreSQL with Managed Identity

Migrate both the `web` and `worker` modules from password-based PostgreSQL authentication
to Azure Database for PostgreSQL with Managed Identity. This removes hardcoded database
credentials from `application.properties` and replaces them with the Spring Cloud Azure
passwordless authentication mechanism.

**Skill**: `migration-mi-postgresql`
