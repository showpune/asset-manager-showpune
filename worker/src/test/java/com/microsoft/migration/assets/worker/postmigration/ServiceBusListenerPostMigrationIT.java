package com.microsoft.migration.assets.worker.postmigration;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.microsoft.migration.assets.worker.model.ImageProcessingMessage;
import com.microsoft.migration.assets.worker.service.AbstractFileProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Post-migration tests for the Azure Service Bus listener in {@link AbstractFileProcessingService}.
 *
 * <p>Verifies that {@code onMessage()} correctly calls {@code sbContext.complete()} on success
 * and {@code sbContext.abandon()} on failure, and that the retry policy fires the expected
 * number of attempts before abandoning.
 *
 * <p>POST-MIGRATION ONLY — added in Phase 3.
 */
@ExtendWith(MockitoExtension.class)
class ServiceBusListenerPostMigrationIT {

    @Mock
    private ServiceBusReceivedMessageContext sbContext;

    private ImageProcessingMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = new ImageProcessingMessage("key.jpg", "image/jpeg", "azure-blob", 100L);
    }

    // ---- helpers ----

    /**
     * Creates an AbstractFileProcessingService that always succeeds (skips processing via wrong storage type).
     */
    private AbstractFileProcessingService buildSucceedingService() throws Exception {
        AbstractFileProcessingService svc = new AbstractFileProcessingService() {
            @Override
            public void downloadOriginal(String key, Path dest) {}
            @Override
            public void uploadThumbnail(Path src, String key, String ct) {}
            @Override
            public String getStorageType() { return "other-type"; } // never matches — skip processing
            @Override
            protected String generateUrl(String key) { return "/test/" + key; }
        };
        injectRetryTemplate(svc, 1); // 1 attempt, no backoff
        return svc;
    }

    /**
     * Creates an AbstractFileProcessingService that always throws on process(), with given retry count.
     */
    private AbstractFileProcessingService buildFailingService(int maxAttempts) throws Exception {
        AbstractFileProcessingService svc = new AbstractFileProcessingService() {
            @Override
            public void downloadOriginal(String key, Path dest) throws Exception {
                throw new Exception("simulated download failure");
            }
            @Override
            public void uploadThumbnail(Path src, String key, String ct) {}
            @Override
            public String getStorageType() { return "azure-blob"; } // matches to trigger actual processing
            @Override
            protected String generateUrl(String key) { return "/test/" + key; }
        };
        injectRetryTemplate(svc, maxAttempts);
        return svc;
    }

    private void injectRetryTemplate(AbstractFileProcessingService svc, int maxAttempts) throws Exception {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(maxAttempts));
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        Field f = AbstractFileProcessingService.class.getDeclaredField("retryTemplate");
        f.setAccessible(true);
        f.set(svc, retryTemplate);
    }

    // ----- happy path -----

    @Test
    void onMessage_success_completesMessage() throws Exception {
        AbstractFileProcessingService svc = buildSucceedingService();

        svc.onMessage(testMessage, sbContext);

        verify(sbContext).complete();
        verify(sbContext, never()).abandon();
    }

    // ----- failure + ack -----

    @Test
    void onMessage_failure_abandonsMessage() throws Exception {
        AbstractFileProcessingService svc = buildFailingService(1);

        svc.onMessage(testMessage, sbContext);

        verify(sbContext).abandon();
        verify(sbContext, never()).complete();
    }

    // ----- retry: exhausted before abandon -----

    @Test
    void onMessage_failure_retriesBeforeAbandoning() throws Exception {
        int maxAttempts = 3;
        int[] callCount = {0};

        AbstractFileProcessingService svc = new AbstractFileProcessingService() {
            @Override
            public void downloadOriginal(String key, Path dest) throws Exception {
                callCount[0]++;
                throw new Exception("simulated failure attempt " + callCount[0]);
            }
            @Override
            public void uploadThumbnail(Path src, String key, String ct) {}
            @Override
            public String getStorageType() { return "azure-blob"; }
            @Override
            protected String generateUrl(String key) { return "/test/" + key; }
        };
        injectRetryTemplate(svc, maxAttempts);

        svc.onMessage(testMessage, sbContext);

        // downloadOriginal must be called exactly maxAttempts times
        assert callCount[0] == maxAttempts :
                "Expected " + maxAttempts + " attempts but got " + callCount[0];
        verify(sbContext).abandon();
        verify(sbContext, never()).complete();
    }

    // ----- null context -----

    @Test
    void onMessage_success_withNullContext_doesNotThrow() throws Exception {
        AbstractFileProcessingService svc = buildSucceedingService();
        // Must not throw even without a context (e.g., local / dev processing)
        svc.onMessage(testMessage, null);
    }
}
