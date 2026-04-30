package com.microsoft.migration.assets.worker.baseline;

import com.microsoft.migration.assets.worker.baseline.mock.MockFileAdapter;
import com.microsoft.migration.assets.worker.baseline.mock.TestFileProcessingService;
import com.microsoft.migration.assets.worker.model.ImageProcessingMessage;
import com.microsoft.migration.assets.worker.service.ImageProcessingAdaptor;
import com.microsoft.migration.assets.worker.util.StorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Baseline integration tests for the worker module, exercising the
 * {@link ImageProcessingAdaptor} boundary.
 *
 * <p>Tests are independent of RabbitMQ, AWS S3, and PostgreSQL — all storage
 * operations are handled by an in-memory {@link MockFileAdapter}.
 *
 * <p>FROZEN after Phase 1 — do not modify.
 */
class WorkerBaselineIT {

    private MockFileAdapter mockAdapter;
    private ImageProcessingAdaptor adaptor;

    @BeforeEach
    void setUp() throws Exception {
        mockAdapter = new MockFileAdapter();

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1)); // no retries for tests
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());

        TestFileProcessingService service = new TestFileProcessingService(mockAdapter);
        // inject RetryTemplate via reflection (field is @Autowired in abstract class)
        java.lang.reflect.Field f = com.microsoft.migration.assets.worker.service.AbstractFileProcessingService.class
                .getDeclaredField("retryTemplate");
        f.setAccessible(true);
        f.set(service, retryTemplate);

        adaptor = service;
    }

    // ----- happy path -----

    @Test
    void process_validJpegMessage_generatesThumbnail() throws Exception {
        String key = mockAdapter.seedFixture("sample-small-jpg");
        String contentType = SharedFixtures.getContentType("sample-small-jpg");

        ImageProcessingMessage message = new ImageProcessingMessage(key, contentType, "mock", 0L);
        adaptor.process(message);

        String expectedThumbKey = StorageUtil.getThumbnailKey(key);
        assertThat(mockAdapter.getUploadedKeys()).contains(expectedThumbKey);
        assertThat(mockAdapter.getStored(expectedThumbKey)).isNotNull();
        assertThat(mockAdapter.getStored(expectedThumbKey).length).isGreaterThan(0);
    }

    @Test
    void process_validPngMessage_generatesThumbnail() throws Exception {
        String key = mockAdapter.seedFixture("sample-medium-png");
        String contentType = SharedFixtures.getContentType("sample-medium-png");

        ImageProcessingMessage message = new ImageProcessingMessage(key, contentType, "mock", 0L);
        adaptor.process(message);

        String expectedThumbKey = StorageUtil.getThumbnailKey(key);
        assertThat(mockAdapter.getUploadedKeys()).contains(expectedThumbKey);
        assertThat(mockAdapter.getStored(expectedThumbKey)).isNotNull();
    }

    // ----- boundary -----

    @Test
    void process_wrongStorageType_skipsProcessing() throws Exception {
        String key = mockAdapter.seedFixture("sample-small-jpg");
        String contentType = SharedFixtures.getContentType("sample-small-jpg");

        // Storage type "s3" does not match the mock's type "mock"
        ImageProcessingMessage message = new ImageProcessingMessage(key, contentType, "s3", 0L);
        adaptor.process(message);

        // No thumbnails should be generated because the storage type doesn't match
        assertThat(mockAdapter.getUploadedKeys()).isEmpty();
    }

    // ----- error handling -----

    @Test
    void process_missingFile_throwsException() {
        ImageProcessingMessage message =
                new ImageProcessingMessage("nonexistent-file.jpg", "image/jpeg", "mock", 0L);

        assertThatThrownBy(() -> adaptor.process(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process image");
    }

    @Test
    void process_nonImageFile_throwsException() throws Exception {
        // Text file cannot be processed as an image
        String key = mockAdapter.seedFixture("sample-unicode-txt");
        String contentType = SharedFixtures.getContentType("sample-unicode-txt");

        ImageProcessingMessage message = new ImageProcessingMessage(key, contentType, "mock", 0L);

        assertThatThrownBy(() -> adaptor.process(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process image");
    }
}
