package com.microsoft.migration.assets.worker.postmigration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Utility for loading Layer 2 post-migration fixtures in worker tests,
 * with compliance verification against Layer 1 shared fixtures.
 */
public final class PostMigrationFixtures {

    private static final Path SHARED_ROOT = resolveSharedRoot();
    private static final Path POSTMIGRATION_ROOT = resolvePostMigrationRoot();

    private PostMigrationFixtures() {}

    @SuppressWarnings("unchecked")
    public static String getExpectedStorageType(String postMigrationId) throws IOException {
        return (String) requireEntry(postMigrationId).get("expectedStorageType");
    }

    @SuppressWarnings("unchecked")
    public static String getExpectedContentType(String postMigrationId) throws IOException {
        Map<String, Object> entry = requireEntry(postMigrationId);
        String expectedContentType = (String) entry.get("expectedContentType");
        String sharedId = (String) entry.get("sharedId");
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

    @SuppressWarnings("unchecked")
    public static String getExpectedUrlPrefix(String postMigrationId) throws IOException {
        return (String) requireEntry(postMigrationId).get("expectedUrlPrefix");
    }

    @SuppressWarnings("unchecked")
    public static byte[] loadPayloadBytes(String postMigrationId) throws IOException {
        Map<String, Object> entry = requireEntry(postMigrationId);
        String sharedId = (String) entry.get("sharedId");
        return com.microsoft.migration.assets.worker.baseline.SharedFixtures.loadPayloadBytes(sharedId);
    }

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
        Map<String, Object> manifest = new ObjectMapper().readValue(manifestPath.toFile(), Map.class);
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
