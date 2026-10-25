package com.sns.marigold.storage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.global.error.FailureReporter;
import com.sns.marigold.global.error.GlobalExceptionHandler;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.service.LocalStorageService;
import com.sns.marigold.storage.service.LocalStorageUrlSigner;
import com.sns.marigold.storage.service.StorageDirectory;
import com.sns.marigold.storage.service.StoredFileHead;
import com.sns.marigold.storage.service.UploadPurpose;

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
            .setControllerAdvice(
                new GlobalExceptionHandler(mock(AuditLogger.class), mock(FailureReporter.class)))
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

  @Test
  void uploadFile_SignedUrl() throws Exception {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    byte[] body = "hello".getBytes(StandardCharsets.UTF_8);

    mockMvc
        .perform(put(URI.create(uploadUrl(storedFileName, body.length))).content(body))
        .andExpect(status().isOk());

    StoredFileHead head = storageService.readFileHead(storedFileName, 1024);
    assertThat(head.size()).isEqualTo(body.length);
    assertThat(head.head()).isEqualTo(body);
  }

  @Test
  void uploadFile_GetSignatureRejected() throws Exception {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    String path = UrlConstants.STORAGE_BASE + "/files/" + storedFileName;
    long expiresAt = Instant.now().plusSeconds(60).getEpochSecond();

    mockMvc
        .perform(
            put(path)
                .param("expires", Long.toString(expiresAt))
                .param("signature", localStorageUrlSigner.sign("GET", path, expiresAt, null))
                .content("hello"))
        .andExpect(status().isForbidden());
  }

  @Test
  void uploadFile_OverPurposeLimit() throws Exception {
    String storedFileName = "user/profile/11111111-1111-1111-1111-111111111111.png";
    byte[] body = new byte[(int) UploadPurpose.USER_PROFILE_IMAGE.maxSizeBytes() + 1];

    mockMvc
        .perform(put(URI.create(uploadUrl(storedFileName, body.length))).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploadFile_DoesNotOverwrite() throws Exception {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    String url = uploadUrl(storedFileName, 5);
    mockMvc.perform(put(URI.create(url)).content("hello")).andExpect(status().isOk());

    mockMvc.perform(put(URI.create(url)).content("other")).andExpect(status().is5xxServerError());

    assertThat(storageService.readFileHead(storedFileName, 1024).head())
        .isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
  }

  private String uploadUrl(String storedFileName, long size) {
    return storageService.createUploadUrl(
        storedFileName, "image/png", size, Duration.ofMinutes(15));
  }

  private String uploadTextFile(StorageDirectory storageDirectory) {
    String storedFileName = storageDirectory.path() + "/" + UUID.randomUUID() + ".txt";
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
    storageService.storeUploadedFile(
        storedFileName, new ByteArrayInputStream(content), content.length);
    return storedFileName;
  }
}
