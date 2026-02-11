# Upgrade Plan

## Overview
This plan upgrades the Java project to the latest Long-Term Support (LTS) versions: Java 21 and Spring Boot 3.4.

## Objectives
- Upgrade to Java 21 (current LTS version)
- Upgrade to Spring Boot 3.4 with Spring Framework 6.x
- Migrate from javax.* to jakarta.* namespace
- Ensure all builds and tests pass after upgrade

## Tasks
Detailed task breakdown and execution tracking available in `tasks.json`.

### Task Sequence
1. **Java Version Upgrade** - Upgrade JDK to version 21
2. **Spring Boot Upgrade** - Upgrade to Spring Boot 3.4 (depends on Java upgrade completion)

## Notes
- Java 21 is the current LTS version with long-term support
- Spring Boot 3.x requires Java 17 minimum, fully compatible with Java 21
- Migration includes mandatory javax.* to jakarta.* package namespace changes
