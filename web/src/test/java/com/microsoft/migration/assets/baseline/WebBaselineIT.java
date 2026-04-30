package com.microsoft.migration.assets.baseline;

import com.microsoft.migration.assets.baseline.mock.MockStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Baseline integration tests for the web module, exercising behavior at the HTTP boundary.
 *
 * <p>All external dependencies (storage, messaging, database) are provided by in-memory mocks.
 * These tests must pass against both the old implementation (baseline) and the new implementation
 * (post-migration), making them the primary regression-detection mechanism.
 *
 * <p>FROZEN after Phase 1 — do not modify.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("baseline")
@EnableAutoConfiguration(exclude = {RabbitAutoConfiguration.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebBaselineIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockStorageService mockStorage;

    @BeforeEach
    void setUp() {
        mockStorage.reset();
    }

    // ----- listObjects -----

    @Test
    @Order(1)
    void listObjects_emptyStorage_returnsListView() throws Exception {
        mockMvc.perform(get("/s3"))
                .andExpect(status().isOk())
                .andExpect(view().name("list"));
    }

    @Test
    @Order(2)
    void listObjects_afterUpload_returnsOneObject() throws Exception {
        byte[] jpgBytes = SharedFixtures.loadPayloadBytes("sample-small-jpg");
        MockMultipartFile file = new MockMultipartFile("file", "sample-small.jpg",
                "image/jpeg", jpgBytes);
        mockMvc.perform(multipart("/s3/upload").file(file))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/s3"))
                .andExpect(status().isOk())
                .andExpect(view().name("list"));

        assertThat(mockStorage.listObjects()).hasSize(1);
    }

    // ----- uploadObject -----

    @Test
    @Order(3)
    void uploadObject_validJpeg_redirectsToList() throws Exception {
        byte[] jpgBytes = SharedFixtures.loadPayloadBytes("sample-small-jpg");
        MockMultipartFile file = new MockMultipartFile("file", "sample-small.jpg",
                "image/jpeg", jpgBytes);

        mockMvc.perform(multipart("/s3/upload").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s3*"));
    }

    @Test
    @Order(4)
    void uploadObject_emptyFile_redirectsToUploadWithError() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg",
                "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/s3/upload").file(emptyFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s3/upload*"));
    }

    @Test
    @Order(5)
    void uploadObject_unicodeFilename_succeeds() throws Exception {
        byte[] txtBytes = SharedFixtures.loadPayloadBytes("sample-unicode-txt");
        MockMultipartFile file = new MockMultipartFile("file", "sample-unicode.txt",
                "text/plain", txtBytes);

        mockMvc.perform(multipart("/s3/upload").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s3*"));

        assertThat(mockStorage.listObjects()).hasSize(1);
    }

    @Test
    @Order(6)
    void uploadObject_recordsProcessingMessage() throws Exception {
        byte[] jpgBytes = SharedFixtures.loadPayloadBytes("sample-small-jpg");
        MockMultipartFile file = new MockMultipartFile("file", "sample-small.jpg",
                "image/jpeg", jpgBytes);

        mockMvc.perform(multipart("/s3/upload").file(file))
                .andExpect(status().is3xxRedirection());

        assertThat(mockStorage.getCapturedMessages()).hasSize(1);
        assertThat(mockStorage.getCapturedMessages().get(0).getContentType()).isEqualTo("image/jpeg");
        assertThat(mockStorage.getCapturedMessages().get(0).getStorageType()).isEqualTo("mock");
        assertThat(mockStorage.getCapturedMessages().get(0).getSize()).isEqualTo(jpgBytes.length);
    }

    // ----- viewObject -----

    @Test
    @Order(7)
    void viewObject_existingKey_returnsBinaryContent() throws Exception {
        String key = mockStorage.seedFixture("sample-small-jpg");
        byte[] expected = SharedFixtures.loadPayloadBytes("sample-small-jpg");

        byte[] actual = mockMvc.perform(get("/s3/view/" + key))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @Order(8)
    void viewObject_nonExistentKey_returns404() throws Exception {
        mockMvc.perform(get("/s3/view/nonexistent-key.jpg"))
                .andExpect(status().isNotFound());
    }

    // ----- viewObjectPage -----

    @Test
    @Order(9)
    void viewObjectPage_existingKey_returnsViewPage() throws Exception {
        String key = mockStorage.seedFixture("sample-small-jpg");

        mockMvc.perform(get("/s3/view-page/" + key))
                .andExpect(status().isOk())
                .andExpect(view().name("view"));
    }

    @Test
    @Order(10)
    void viewObjectPage_nonExistentKey_redirectsToListWithError() throws Exception {
        mockMvc.perform(get("/s3/view-page/nonexistent-key.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s3*"));
    }

    // ----- deleteObject -----

    @Test
    @Order(11)
    void deleteObject_existingKey_redirectsToList() throws Exception {
        String key = mockStorage.seedFixture("sample-small-jpg");

        mockMvc.perform(post("/s3/delete/" + key))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s3*"));

        assertThat(mockStorage.listObjects()).isEmpty();
    }

    @Test
    @Order(12)
    void deleteObject_nonExistentKey_redirectsToListWithError() throws Exception {
        mockMvc.perform(post("/s3/delete/nonexistent-key.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s3*"));
    }

    // ----- upload GET form -----

    @Test
    @Order(13)
    void uploadForm_returnsUploadView() throws Exception {
        mockMvc.perform(get("/s3/upload"))
                .andExpect(status().isOk())
                .andExpect(view().name("upload"));
    }
}
