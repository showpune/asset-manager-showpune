package com.microsoft.migration.assets.service;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import com.microsoft.migration.assets.model.ImageProcessingMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class to verify Azure Service Bus message publishing works correctly
 */
class LocalFileStorageServiceTest {

    @Mock
    private ServiceBusTemplate serviceBusTemplate;

    private LocalFileStorageService localFileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        localFileStorageService = new LocalFileStorageService(serviceBusTemplate);
        // Set the storage directory to our temp directory for testing
        ReflectionTestUtils.setField(localFileStorageService, "storageDirectory", tempDir.toString());
        // Initialize the service
        try {
            ReflectionTestUtils.invokeMethod(localFileStorageService, "init");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void uploadObject_shouldSendMessageToServiceBus() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "test-file.jpg", 
            "test-file.jpg", 
            "image/jpeg", 
            "test content".getBytes()
        );

        // When
        localFileStorageService.uploadObject(file);

        // Then
        verify(serviceBusTemplate, times(1))
            .send(eq("image-processing"), any(Message.class));
    }
}