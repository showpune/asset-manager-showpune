# Modernization Plan: Upgrade to Latest JDK

**Project**: Asset Manager Kit

---

## Technical Framework

- **Language**: Java 11
- **Framework**: Spring Boot 2.7.14
- **Build Tool**: Maven
- **Database**: Not specified in current analysis
- **Key Dependencies**: Spring Boot ecosystem

---

## Overview

This migration upgrades the Java Development Kit (JDK) from version 11 to version 21. The application currently runs on Java 11, which while still supported, lacks the performance improvements, language features, and security enhancements available in Java 21. The new version will:

- Improve application performance through JVM optimizations
- Provide access to modern Java language features (records, pattern matching, etc.)
- Ensure long-term support with the latest LTS release

The migration follows a single-phase approach to upgrade the JDK version across all project modules.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| Asset Manager Kit | Java 11 | Java 21 | N/A | Upgrade JDK to latest LTS version |

---

## Clarifications

The following items were not explicitly requested but may be needed for a complete implementation:

1. **Spring Boot Compatibility**: 
   - **Why needed**: Spring Boot 2.7.14 is compatible with Java 11-17. For Java 21 support, Spring Boot 3.x is recommended.
   - **Options**: 
     - Upgrade only JDK to Java 17 (maximum supported by Spring Boot 2.7.14)
     - Upgrade to Spring Boot 3.x along with Java 21 (includes Jakarta EE migration)
   - **Recommendation**: If you want Java 21, consider upgrading to Spring Boot 3.x first. Otherwise, Java 17 is the maximum for your current Spring Boot version.

2. **Dependencies Compatibility**:
   - **Why needed**: Some third-party libraries may not be compatible with Java 21
   - **Options**: Review and update dependencies during the upgrade
   - **Recommendation**: Test thoroughly after JDK upgrade and update incompatible dependencies
