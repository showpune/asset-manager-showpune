package com.microsoft.migration.assets.service;

import com.microsoft.migration.assets.model.S3StorageItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class LocalFileStorageServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private LocalFileStorageService service;

    @TempDir
    Path tempDir;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        service = new LocalFileStorageService(rabbitTemplate);
        ReflectionTestUtils.setField(service, "storageDirectory", tempDir.toString());
        service.init();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testListObjectsWhenEmpty() {
        List<S3StorageItem> objects = service.listObjects();
        assertNotNull(objects);
        assertTrue(objects.isEmpty());
    }

    @Test
    void testListObjectsWithFiles() throws IOException {
        // Create test files
        Files.writeString(tempDir.resolve("test1.txt"), "content1");
        Files.writeString(tempDir.resolve("test2.txt"), "content2");

        List<S3StorageItem> objects = service.listObjects();
        assertNotNull(objects);
        assertEquals(2, objects.size());
    }

    @Test
    void testUploadObject() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        service.uploadObject(file);

        // Verify file was created
        Path uploadedFile = tempDir.resolve("test.txt");
        assertTrue(Files.exists(uploadedFile));
        assertEquals("test content", Files.readString(uploadedFile));

        // Verify message was sent
        verify(rabbitTemplate).convertAndSend(eq("image-processing"), any(Object.class));
    }

    @Test
    void testUploadEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            new byte[0]
        );

        assertThrows(IOException.class, () -> service.uploadObject(file));
    }

    @Test
    void testUploadFileWithInvalidPath() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "../malicious.txt",
            "text/plain",
            "content".getBytes()
        );

        assertThrows(IOException.class, () -> service.uploadObject(file));
    }

    @Test
    void testGetObject() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        try (InputStream inputStream = service.getObject("test.txt")) {
            assertNotNull(inputStream);
            String content = new String(inputStream.readAllBytes());
            assertEquals("test content", content);
        }
    }

    @Test
    void testGetObjectNotFound() {
        assertThrows(FileNotFoundException.class, () -> service.getObject("nonexistent.txt"));
    }

    @Test
    void testDeleteObject() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
        assertTrue(Files.exists(testFile));

        service.deleteObject("test.txt");
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testDeleteObjectNotFound() {
        assertThrows(FileNotFoundException.class, () -> service.deleteObject("nonexistent.txt"));
    }

    @Test
    void testDeleteObjectWithThumbnail() throws IOException {
        // Create original and thumbnail files
        Path originalFile = tempDir.resolve("test.jpg");
        Path thumbnailFile = tempDir.resolve("test_thumbnail.jpg");
        Files.writeString(originalFile, "original");
        Files.writeString(thumbnailFile, "thumbnail");

        service.deleteObject("test.jpg");

        assertFalse(Files.exists(originalFile));
        assertFalse(Files.exists(thumbnailFile));
    }

    @Test
    void testGetStorageType() {
        assertEquals("local", service.getStorageType());
    }
}
