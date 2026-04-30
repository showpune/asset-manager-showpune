package com.microsoft.migration.assets.worker.baseline;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Utility for loading testdata/shared fixtures in worker baseline tests.
 * References the same frozen testdata/shared/ directory used by web baseline tests.
 */
public final class SharedFixtures {

    private static final Path TESTDATA_ROOT = resolveTestdataRoot();

    private SharedFixtures() {}

    public static Path getTestdataRoot() {
        return TESTDATA_ROOT;
    }

    @SuppressWarnings("unchecked")
    public static byte[] loadPayloadBytes(String fixtureId) throws IOException {
        Map<String, Object> manifest = loadManifest();
        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) manifest.get("fixtures");

        Map<String, Object> entry = fixtures.stream()
                .filter(f -> fixtureId.equals(f.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fixture not found: " + fixtureId));

        String filePath = (String) entry.get("filePath");
        String expectedSha256 = (String) entry.get("sha256");

        Path payloadPath = TESTDATA_ROOT.resolve(filePath);
        byte[] bytes = Files.readAllBytes(payloadPath);

        String actualSha256 = sha256Hex(bytes);
        if (!actualSha256.equals(expectedSha256)) {
            throw new IllegalStateException(
                    "SHA-256 mismatch for fixture '" + fixtureId + "': expected=" + expectedSha256 + " actual=" + actualSha256);
        }
        return bytes;
    }

    @SuppressWarnings("unchecked")
    public static String getContentType(String fixtureId) throws IOException {
        Map<String, Object> manifest = loadManifest();
        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) manifest.get("fixtures");
        return (String) fixtures.stream()
                .filter(f -> fixtureId.equals(f.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fixture not found: " + fixtureId))
                .get("contentType");
    }

    @SuppressWarnings("unchecked")
    public static String getFilename(String fixtureId) throws IOException {
        Map<String, Object> manifest = loadManifest();
        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) manifest.get("fixtures");
        String filePath = (String) fixtures.stream()
                .filter(f -> fixtureId.equals(f.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fixture not found: " + fixtureId))
                .get("filePath");
        return Paths.get(filePath).getFileName().toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadManifest() throws IOException {
        Path manifestPath = TESTDATA_ROOT.resolve("manifest.json");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(manifestPath.toFile(), Map.class);
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static Path resolveTestdataRoot() {
        Path moduleDir = Paths.get(System.getProperty("user.dir"));
        return moduleDir.getParent().resolve("testdata/shared").normalize();
    }
}
