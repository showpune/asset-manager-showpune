# Modernization Plan: Upgrade to Java 21

**Project**: Asset Manager Kit

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Spring Boot 2.7.14
- **Build Tool**: Maven 3.x
- **Database**: Not specified
- **Key Dependencies**: Spring Boot Starter Parent

---

## Overview

This migration upgrades the Asset Manager Kit application from Java 11 to Java 21. The application currently runs on Java 11 with Spring Boot 2.7.14. The new architecture will:

- Upgrade the JDK from version 11 to version 21, incorporating all language improvements and performance enhancements from Java 12 through 21
- Update dependencies and configurations to ensure compatibility with Java 21
- Maintain full backward compatibility with existing application functionality

The migration follows a focused approach targeting only the JDK upgrade as specified by the user.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| Asset Manager Kit | Java 11 | Java 21 | N/A | Upgrade JDK to version 21 |

---

## Clarifications

The following items were not explicitly requested but may be considered for a complete modernization:

1. **Spring Boot Upgrade**: The project currently uses Spring Boot 2.7.14, which is compatible with Java 11-17 but not fully optimized for Java 21
   - **Why needed**: Spring Boot 3.x provides native support for Java 21 and includes Jakarta EE migration
   - **Options**: 
     - Keep Spring Boot 2.7.14 (may require additional compatibility adjustments)
     - Upgrade to Spring Boot 3.x for full Java 21 support
   - **Recommendation**: If issues arise during Java 21 upgrade with current Spring Boot version, consider upgrading Spring Boot in a separate task

2. **Testing Strategy**: No specific testing requirements were provided
   - **Why needed**: Ensure application behavior remains consistent after JDK upgrade
   - **Options**: 
     - Run existing tests only
     - Generate additional integration tests
   - **Recommendation**: Run existing tests to verify backward compatibility
