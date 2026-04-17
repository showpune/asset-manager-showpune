# Modernization Summary: 001-upgrade-java-to-21

## Task
Upgrade Java runtime from Java 17 to Java 21 across all modules.

## Changes Made

### `pom.xml` (parent)
- Updated `<java.version>` property from `17` to `21`.

Both child modules (`web` and `worker`) inherit the Java version from the parent, so no changes were needed in their respective `pom.xml` files.

## Verification

| Check | Result |
|-------|--------|
| Build (`mvn package`) | ✅ Passed |
| Unit Tests | ✅ Passed |
| Java 17 references remaining | None |

## Notes
- JDK 21 (Temurin) was available on the build system at `/usr/lib/jvm/temurin-21-jdk-amd64`.
- Spring Boot 3.2.1 (already in use) is fully compatible with Java 21.
- No source code changes were required; the version bump in the parent POM is sufficient.
