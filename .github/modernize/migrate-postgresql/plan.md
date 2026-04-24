# Modernization Plan: Migrate PostgreSQL

**Project**: Asset Manager (assets-manager-showpune)

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL (local/self-hosted, password-based auth)
- **Key Dependencies**: Spring Data JPA, Hibernate, PostgreSQL JDBC Driver

---

## Overview

This migration moves the Asset Manager application from a locally-hosted PostgreSQL
instance to Azure Database for PostgreSQL. The application currently uses
password-based authentication with hardcoded credentials in `application.properties`.
The new architecture will:

- Replace locally-hosted PostgreSQL with Azure Database for PostgreSQL for managed,
  scalable, and highly available database infrastructure
- Enable passwordless authentication using Azure Managed Identity, eliminating
  hardcoded credentials and meeting organizational security requirements
- Comply with organizational policies prohibiting hardcoded credentials and mandating
  Managed Identity for service-to-service authentication (per Acme Corp Modernization
  Playbook)

The migration follows a single-phase approach, updating Spring Cloud Azure dependencies
and datasource configuration in both the `web` and `worker` modules.

---

## Migration Impact Summary

| Application           | Original Service        | New Azure Service                  | Authentication   | Comments                                                         |
|-----------------------|-------------------------|------------------------------------|------------------|------------------------------------------------------------------|
| assets-manager-web    | PostgreSQL (local)      | Azure Database for PostgreSQL      | Managed Identity | Migrate to Azure Database for PostgreSQL with passwordless auth  |
| assets-manager-worker | PostgreSQL (local)      | Azure Database for PostgreSQL      | Managed Identity | Migrate to Azure Database for PostgreSQL with passwordless auth  |

---

## Migration Tasks

### Task 001 — Migrate PostgreSQL to Azure Database for PostgreSQL with Managed Identity

Migrate both the `web` and `worker` modules from password-based local PostgreSQL
to Azure Database for PostgreSQL using Managed Identity for passwordless authentication.
This removes hardcoded credentials and aligns with the organizational security policy
requiring Managed Identity for all service-to-service authentication.

---

## Playbook Policy Compliance

This plan follows the **Acme Corp Modernization Playbook** (v1.0) policies:

| Policy | Status | Notes |
|--------|--------|-------|
| Java 17 target | ✅ Already compliant | Project uses Java 17 |
| Spring Boot 3.x target | ✅ Already compliant | Project uses Spring Boot 3.2.1 |
| Managed Identity for service auth | ✅ Addressed by Task 001 | Replaces hardcoded password-based auth |
| No hardcoded credentials | ✅ Addressed by Task 001 | Removes `postgres/postgres` from properties |
| Azure Key Vault for secrets | ✅ Addressed by Task 001 | Passwordless via Managed Identity |
