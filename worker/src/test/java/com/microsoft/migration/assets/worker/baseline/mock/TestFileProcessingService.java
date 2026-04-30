package com.microsoft.migration.assets.worker.baseline.mock;

import com.microsoft.migration.assets.worker.service.AbstractFileProcessingService;

import java.nio.file.Path;

/**
 * Concrete subclass of AbstractFileProcessingService used only in baseline tests.
 * Delegates download/upload to an injected MockFileAdapter, keeping the test
 * entirely within in-memory storage without touching S3 or the filesystem.
 */
public class TestFileProcessingService extends AbstractFileProcessingService {

    private final MockFileAdapter adapter;

    public TestFileProcessingService(MockFileAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void downloadOriginal(String key, Path destination) throws Exception {
        adapter.downloadOriginal(key, destination);
    }

    @Override
    public void uploadThumbnail(Path source, String key, String contentType) throws Exception {
        adapter.uploadThumbnail(source, key, contentType);
    }

    @Override
    public String getStorageType() {
        return adapter.getStorageType();
    }

    @Override
    protected String generateUrl(String key) {
        return "/mock/view/" + key;
    }
}
