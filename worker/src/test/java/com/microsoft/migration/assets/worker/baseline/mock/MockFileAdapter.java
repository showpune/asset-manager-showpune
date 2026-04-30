package com.microsoft.migration.assets.worker.baseline.mock;

import com.microsoft.migration.assets.worker.baseline.SharedFixtures;
import com.microsoft.migration.assets.worker.service.FileProcessor;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory FileProcessor used by worker baseline tests.
 * Provides download/upload operations backed by a ConcurrentHashMap.
 * Does not import any AWS SDK or RabbitMQ types.
 */
public class MockFileAdapter implements FileProcessor {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();
    private final List<String> uploadedKeys = Collections.synchronizedList(new ArrayList<>());

    /** Reset all stored data. Call in @BeforeEach. */
    public void reset() {
        store.clear();
        uploadedKeys.clear();
    }

    /** Seed the in-memory store from a testdata/shared fixture. Returns the key used. */
    public String seedFixture(String fixtureId) throws Exception {
        byte[] bytes = SharedFixtures.loadPayloadBytes(fixtureId);
        String filename = SharedFixtures.getFilename(fixtureId);
        store.put(filename, bytes);
        return filename;
    }

    /** Returns all thumbnail keys uploaded during test execution. */
    public List<String> getUploadedKeys() {
        return Collections.unmodifiableList(new ArrayList<>(uploadedKeys));
    }

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        byte[] data = store.get(key);
        if (data == null) {
            throw new FileNotFoundException("File not found in mock store: " + key);
        }
        Files.write(destination, data);
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        byte[] data = Files.readAllBytes(source);
        store.put(key, data);
        uploadedKeys.add(key);
    }

    @Override
    public String getStorageType() {
        return "mock";
    }

    /** Returns bytes stored under given key, or null if absent. */
    public byte[] getStored(String key) {
        return store.get(key);
    }
}
