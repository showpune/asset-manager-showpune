package com.microsoft.migration.assets.baseline.mock;

import com.microsoft.migration.assets.baseline.SharedFixtures;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import com.microsoft.migration.assets.model.S3StorageItem;
import com.microsoft.migration.assets.service.StorageService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory StorageService mock used exclusively during baseline integration tests.
 * Does not depend on AWS S3, RabbitMQ, or any PostgreSQL connection.
 * Implements the StorageService adaptor interface using only standard Java types.
 *
 * <p>Seeded from testdata/shared/manifest.json via {@link SharedFixtures}.
 */
@Service
@Profile("baseline")
public class MockStorageService implements StorageService {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();
    private final Map<String, String> contentTypes = new ConcurrentHashMap<>();
    private final Map<String, Instant> uploadTimes = new ConcurrentHashMap<>();
    private final List<ImageProcessingMessage> capturedMessages =
            Collections.synchronizedList(new ArrayList<>());

    /** Clears all stored objects and recorded messages. Call in @BeforeEach. */
    public void reset() {
        store.clear();
        contentTypes.clear();
        uploadTimes.clear();
        capturedMessages.clear();
    }

    /** Seed mock with a fixture from testdata/shared by its manifest id. */
    public String seedFixture(String fixtureId) throws IOException {
        byte[] bytes = SharedFixtures.loadPayloadBytes(fixtureId);
        String contentType = SharedFixtures.getContentType(fixtureId);
        String filename = SharedFixtures.getFilename(fixtureId);
        String key = UUID.randomUUID() + "-" + filename;
        store.put(key, bytes);
        contentTypes.put(key, contentType);
        uploadTimes.put(key, Instant.now());
        return key;
    }

    /** Returns a snapshot of messages that were "sent to the queue" during uploadObject calls. */
    public List<ImageProcessingMessage> getCapturedMessages() {
        return Collections.unmodifiableList(new ArrayList<>(capturedMessages));
    }

    @Override
    public List<S3StorageItem> listObjects() {
        List<S3StorageItem> items = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : store.entrySet()) {
            String key = e.getKey();
            Instant uploadedAt = uploadTimes.getOrDefault(key, Instant.now());
            items.add(new S3StorageItem(key, extractFilename(key), e.getValue().length,
                    uploadedAt, uploadedAt, "/s3/view/" + key));
        }
        return items;
    }

    @Override
    public void uploadObject(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }
        String key = UUID.randomUUID() + "-" + file.getOriginalFilename();
        byte[] bytes = file.getBytes();
        store.put(key, bytes);
        contentTypes.put(key, file.getContentType());
        uploadTimes.put(key, Instant.now());

        capturedMessages.add(new ImageProcessingMessage(key, file.getContentType(),
                getStorageType(), file.getSize()));
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        byte[] data = store.get(key);
        if (data == null) {
            throw new FileNotFoundException("Object not found: " + key);
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public void deleteObject(String key) throws IOException {
        if (!store.containsKey(key)) {
            throw new FileNotFoundException("Object not found: " + key);
        }
        store.remove(key);
        contentTypes.remove(key);
        uploadTimes.remove(key);
    }

    @Override
    public String getStorageType() {
        return "mock";
    }

    private String extractFilename(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }
}
