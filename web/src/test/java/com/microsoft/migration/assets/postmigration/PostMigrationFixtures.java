package com.microsoft.migration.assets.postmigration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Utility for loading Layer 2 post-migration fixtures and verifying their compliance
 * against the referenced Layer 1 shared fixtures.
 *
 * <p>Layer 2 compliance rule: {@code expectedContentType} in the post-migration manifest
 * must equal {@code contentType} in the referenced shared manifest entry.
 */
public final class PostMigrationFixtures {

    private static final Path SHARED_ROOT = resolveSharedRoot();
    private static final Path POSTMIGRATION_ROOT = resolvePostMigrationRoot();

    private PostMigrationFixtures() {}

    /**
     * Returns the expected storage type for a post-migration fixture id.
     */
    @SuppressWarnings("unchecked")
    public static String getExpectedStorageType(String postMigrationId) throws IOException {
        Map<String, Object> entry = requireEntry(postMigrationId);
        return (String) entry.get("expectedStorageType");
    }

    /**
     * Returns the expected content-type for a post-migration fixture id.
     * Also validates compliance: the value must match the shared fixture content-type.
     */
    @SuppressWarnings("unchecked")
    public static String getExpectedContentType(String postMigrationId) throws IOException {
        Map<String, Object> entry = requireEntry(postMigrationId);
        String expectedContentType = (String) entry.get("expectedContentType");
        String sharedId = (String) entry.get("sharedId");

        // Layer 1 ↔ Layer 2 compliance check
        String sharedContentType = loadSharedContentType(sharedId);
        if (!expectedContentType.equals(sharedContentType)) {
            throw new IllegalStateException(
                    "Layer 2 compliance failure for '" + postMigrationId + "': "
                    + "expectedContentType=" + expectedContentType
                    + " does not match shared fixture '" + sharedId
                    + "' contentType=" + sharedContentType);
        }
        return expectedContentType;
    }

    /**
     * Returns the expected URL prefix for a post-migration fixture id.
     */
    @SuppressWarnings("unchecked")
    public static String getExpectedUrlPrefix(String postMigrationId) throws IOException {
        return (String) requireEntry(postMigrationId).get("expectedUrlPrefix");
    }

    /**
     * Loads payload bytes for a post-migration fixture via its referenced shared fixture.
     */
    @SuppressWarnings("unchecked")
    public static byte[] loadPayloadBytes(String postMigrationId) throws IOException {
        Map<String, Object> entry = requireEntry(postMigrationId);
        String sharedId = (String) entry.get("sharedId");
        return com.microsoft.migration.assets.baseline.SharedFixtures.loadPayloadBytes(sharedId);
    }

    /**
     * Returns the shared fixture id referenced by a post-migration fixture.
     */
    @SuppressWarnings("unchecked")
    public static String getSharedId(String postMigrationId) throws IOException {
        return (String) requireEntry(postMigrationId).get("sharedId");
    }

    // ---- private helpers ----

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireEntry(String postMigrationId) throws IOException {
        Map<String, Object> manifest = loadPostMigrationManifest();
        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) manifest.get("fixtures");
        return fixtures.stream()
                .filter(f -> postMigrationId.equals(f.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Post-migration fixture not found: " + postMigrationId));
    }

    @SuppressWarnings("unchecked")
    private static String loadSharedContentType(String sharedId) throws IOException {
        Path manifestPath = SHARED_ROOT.resolve("manifest.json");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> manifest = mapper.readValue(manifestPath.toFile(), Map.class);
        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) manifest.get("fixtures");
        return (String) fixtures.stream()
                .filter(f -> sharedId.equals(f.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Shared fixture not found: " + sharedId))
                .get("contentType");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadPostMigrationManifest() throws IOException {
        Path manifestPath = POSTMIGRATION_ROOT.resolve("manifest.json");
        return new ObjectMapper().readValue(manifestPath.toFile(), Map.class);
    }

    private static Path resolveSharedRoot() {
        return Paths.get(System.getProperty("user.dir")).getParent()
                .resolve("testdata/shared").normalize();
    }

    private static Path resolvePostMigrationRoot() {
        return Paths.get(System.getProperty("user.dir")).getParent()
                .resolve("testdata/postmigration").normalize();
    }
}
