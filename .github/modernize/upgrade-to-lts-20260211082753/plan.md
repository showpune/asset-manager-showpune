# Upgrade Plan

## Overview
Upgrade Java project to the latest LTS versions (Java 21 and Spring Boot 3.4).

## Objectives
- Upgrade Java to version 21 (latest LTS)
- Upgrade Spring Boot to version 3.4 (latest stable release)
- Ensure all dependencies and configurations are compatible with the target versions
- Migrate from javax.* to jakarta.* namespace as required by Spring Boot 3.x

## Tasks
See `tasks.json` for detailed task breakdown and execution order.

## Notes
- Java 21 is the latest Long-Term Support (LTS) version
- Spring Boot 3.4 requires Java 17 or higher and uses Jakarta EE 9+ (jakarta.* namespace)
- Tasks are executed sequentially based on dependencies
