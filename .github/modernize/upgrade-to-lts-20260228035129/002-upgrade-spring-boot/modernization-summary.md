# Modernization Summary: 002-upgrade-spring-boot

## Task Description
Upgrade Spring Boot from 3.2.1 to 3.4.x (latest stable: 3.4.5), including all transitive Spring Framework and dependency upgrades.

## Status: ✅ Completed

## Changes Made

### 1. Spring Boot Version Upgrade (pom.xml)
- Upgraded `spring-boot-starter-parent` from **3.2.1** → **3.4.5**
- Added `postgresql.version` property override to **42.7.7** (fix for CVE-2025-49146)

### 2. OpenRewrite-Driven Code Changes (3.2 → 3.3)
Applied `org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_3` recipe which made the following changes:

#### web/src/main/java/…/config/AwsS3Config.java
- Removed `public` modifier from `@Bean` method `s3Client()` (Spring recommendation for 3.3+)

#### web/src/main/java/…/config/RabbitConfig.java
- Removed `public` modifier from `@Bean` methods: `imageProcessingQueue()`, `jsonMessageConverter()`, `rabbitListenerContainerFactory()`

#### web/src/main/java/…/controller/S3Controller.java
- Changed `@RequestParam("file")` to `@RequestParam` on `uploadObject()` — parameter name inferred via `-parameters` compiler flag (Spring Boot 3.x default)

#### worker/src/main/java/…/config/AwsS3Config.java
- Removed `public` modifier from `@Bean` method `s3Client()`

#### worker/src/main/java/…/config/RabbitConfig.java
- Removed `public` modifier from `@Bean` methods: `imageProcessingQueue()`, `jsonMessageConverter()`, `rabbitListenerContainerFactory()`, `retryTemplate()`

### 3. Security Fix
- **CVE-2025-49146** (HIGH): `org.postgresql:postgresql` upgraded from 42.6.0 → **42.7.7**
  - Fixes: pgjdbc channel binding bypass vulnerability allowing MITM attacks

## Dependency Version Changes

| Dependency | Before | After |
|---|---|---|
| spring-boot-starter-parent | 3.2.1 | 3.4.5 |
| spring-framework (transitive) | ~6.1.x | ~6.2.x |
| org.postgresql:postgresql | 42.6.0 | 42.7.7 |
| org.projectlombok:lombok | 1.18.30 | 1.18.38 |
| com.fasterxml.jackson.core:jackson-databind | 2.15.3 | 2.18.3 |
| com.h2database:h2 | 2.2.224 | 2.3.232 |

## Migration Approach
Used a milestone-based upgrade:
- **Milestone 1**: 3.2.1 → 3.3.13 via OpenRewrite recipe `UpgradeSpringBoot_3_3`
- **Milestone 2**: 3.3.13 → 3.4.5 via direct pom.xml version update

## Validation Results
- ✅ Build: **PASSED**
- ✅ Unit Tests: **PASSED** (1/1)
- ✅ CVE Scan: **No vulnerabilities** after postgresql upgrade
- ✅ Code Behavior: No critical/major behavior changes detected

## Notes
- `javax.imageio.*` imports in `AbstractFileProcessingService.java` are from Java SE (not Jakarta EE) and do not require migration
- No Spring Boot configuration property renames were required for this project
- Working branch: `copilot/execute-upgrade-plan-yet-again`
