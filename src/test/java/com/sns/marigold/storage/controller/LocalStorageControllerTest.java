package com.sns.marigold.storage.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.global.error.GlobalExceptionHandler;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.service.LocalStorageService;
import com.sns.marigold.storage.service.LocalStorageUrlSigner;
import com.sns.marigold.storage.service.StorageDirectory;

class LocalStorageControllerTest {

  @TempDir Path tempDir;

  private LocalStorageService storageService;
  private LocalStorageUrlSigner localStorageUrlSigner;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalStorageProperties properties =
        new LocalStorageProperties(
            tempDir.toString(), "http://localhost:8080", "test-signing-secret", 60, 10);
    localStorageUrlSigner = new LocalStorageUrlSigner(properties);
    storageService = new LocalStorageService(properties, localStorageUrlSigner);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new LocalStorageController(storageService, localStorageUrlSigner))
            .setControllerAdvice(new GlobalExceptionHandler(mock(AuditLogger.class)))
            .build();
  }

  @Test
  void viewFile_DomainPath() throws Exception {
    String storedFileName = uploadTextFile(StorageDirectory.ADOPTION_POST);

    mockMvc
        .perform(get(URI.create(storageService.getViewUrlOrNull(storedFileName))))
        .andExpect(status().isOk())
        .andExpect(content().string("hello"));
  }

  @Test
  void downloadFile_DomainPath() throws Exception {
    String storedFileName = uploadTextFile(StorageDirectory.CHAT_ATTACHMENT);

    mockMvc
        .perform(get(URI.create(storageService.getDownloadUrl(storedFileName, "memo.txt"))))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("memo.txt")));
  }

  @Test
  void viewFile_UnsignedUrl() throws Exception {
    String storedFileName = uploadTextFile(StorageDirectory.ADOPTION_POST);

    mockMvc
        .perform(get(UrlConstants.STORAGE_BASE + "/files/" + storedFileName))
        .andExpect(status().isForbidden());
  }

  @Test
  void viewFile_ExpiredUrl() throws Exception {
    String storedFileName = uploadTextFile(StorageDirectory.ADOPTION_POST);
    String path = UrlConstants.STORAGE_BASE + "/files/" + storedFileName;
    long expiresAt = Instant.now().minusSeconds(1).getEpochSecond();

    mockMvc
        .perform(
            get(path)
                .param("expires", Long.toString(expiresAt))
                .param("signature", localStorageUrlSigner.sign("GET", path, expiresAt, null)))
        .andExpect(status().isForbidden());
  }

  @Test
  void viewFile_TamperedPath() throws Exception {
    String storedFileName = uploadTextFile(StorageDirectory.ADOPTION_POST);
    String signedUrl = storageService.getViewUrlOrNull(storedFileName);

    mockMvc
        .perform(get(URI.create(signedUrl.replace("/adoption/post/", "/chat/attachment/"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void downloadFile_TamperedFilename() throws Exception {
    String storedFileName = uploadTextFile(StorageDirectory.CHAT_ATTACHMENT);
    String signedUrl = storageService.getDownloadUrl(storedFileName, "memo.txt");

    mockMvc
        .perform(get(URI.create(signedUrl.replace("filename=memo.txt", "filename=other.txt"))))
        .andExpect(status().isForbidden());
  }

  private String uploadTextFile(StorageDirectory storageDirectory) {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "memo.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
    FileUploadDto uploadedFile = storageService.uploadFiles(List.of(file), storageDirectory).get(0);
    return uploadedFile.getStoredFileName();
  }
}
