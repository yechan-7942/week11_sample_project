package edu.handong.web11.controller;

import edu.handong.web11.domain.Attachment;
import edu.handong.web11.repository.AttachmentRepository;
import edu.handong.web11.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttachmentControllerTest {
    @TempDir
    Path uploadDir;

    private AttachmentRepository attachments;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        attachments = mock(AttachmentRepository.class);
        FileStorageService storage = new FileStorageService(uploadDir.toString());
        mvc = MockMvcBuilders.standaloneSetup(new AttachmentController(attachments, storage)).build();
    }

    @ParameterizedTest
    @CsvSource({"jpg,image/jpeg", "jpeg,image/jpeg", "png,image/png", "gif,image/gif"})
    void previewReturnsFileWithImageContentType(String extension, String contentType) throws Exception {
        byte[] bytes = {1, 2, 3};
        String name = "sample." + extension;
        Files.write(uploadDir.resolve(name), bytes);
        when(attachments.findById(1L)).thenReturn(Optional.of(new Attachment(name, name, contentType, bytes.length)));

        mvc.perform(get("/attachments/1/view"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(header().doesNotExist("Content-Disposition"))
            .andExpect(content().bytes(bytes));
    }

    @Test
    void pdfPreviewIsRejectedButDownloadStillWorks() throws Exception {
        byte[] bytes = {1, 2, 3};
        Files.write(uploadDir.resolve("sample.pdf"), bytes);
        when(attachments.findById(1L)).thenReturn(Optional.of(
            new Attachment("sample.pdf", "sample.pdf", "application/pdf", bytes.length)));

        mvc.perform(get("/attachments/1/view")).andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/attachments/1/download"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
            .andExpect(content().bytes(bytes));
    }

    @Test
    void unknownIdReturnsNotFound() throws Exception {
        when(attachments.findById(999L)).thenReturn(Optional.empty());
        mvc.perform(get("/attachments/999/view")).andExpect(status().isNotFound());
    }

    @Test
    void missingPhysicalFileReturnsNotFound() throws Exception {
        when(attachments.findById(1L)).thenReturn(Optional.of(
            new Attachment("missing.png", "missing.png", "image/png", 10)));
        mvc.perform(get("/attachments/1/view")).andExpect(status().isNotFound());
    }

    @Test
    void pathOutsideUploadDirectoryIsRejected() throws Exception {
        when(attachments.findById(1L)).thenReturn(Optional.of(
            new Attachment("outside.png", "../outside.png", "image/png", 10)));
        mvc.perform(get("/attachments/1/view")).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdReturnsBadRequest() throws Exception {
        mvc.perform(get("/attachments/abc/view")).andExpect(status().isBadRequest());
    }
}
