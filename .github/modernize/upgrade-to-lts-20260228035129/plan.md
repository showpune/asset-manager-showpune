# Upgrade Plan: Upgrade to Latest LTS Versions

## Overview
This plan upgrades the Java application to the latest Long-Term Support (LTS) versions: Java 21 and Spring Boot 3.4.

## Objectives
- Upgrade Java to version 21 (latest LTS)
- Upgrade Spring Boot to version 3.4 with Spring Framework 6.x
- Migrate from javax.* to jakarta.* namespace
- Ensure all tests pass and the application builds successfully

## Tasks
See `tasks.json` for the detailed task breakdown and execution plan.

## Migration Path
1. **Java 21 Upgrade** - Update JDK version and resolve Java API compatibility
2. **Spring Boot 3.4 Upgrade** - Migrate to latest Spring Boot with Jakarta EE namespace changes

## Success Criteria
- Project compiles successfully after each upgrade task
- All existing unit tests pass
- No regression in functionality
