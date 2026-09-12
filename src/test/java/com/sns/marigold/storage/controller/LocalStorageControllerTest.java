package com.sns.marigold.storage.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.service.LocalStorageService;
import com.sns.marigold.storage.service.StorageDirectory;

class LocalStorageControllerTest {

  @TempDir Path tempDir;

  private LocalStorageService storageService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    storageService =
        new LocalStorageService(
            new LocalStorageProperties(tempDir.toString(), "http://localhost:8080"));
    mockMvc = MockMvcBuilders.standaloneSetup(new LocalStorageController(storageService)).build();
  }

  @Test
  void viewFile_DomainPath() throws Exception {
    String fileName = uploadTextFile(StorageDirectory.ADOPTION_POST);

    mockMvc
        .perform(get(UrlConstants.STORAGE_BASE + "/files/adoption/post/{fileName}", fileName))
        .andExpect(status().isOk())
        .andExpect(content().string("hello"));
  }

  @Test
  void downloadFile_DomainPath() throws Exception {
    String fileName = uploadTextFile(StorageDirectory.CHAT_ATTACHMENT);

    mockMvc
        .perform(
            get(UrlConstants.STORAGE_BASE + "/files/chat/attachment/{fileName}/download", fileName)
                .param("filename", "memo.txt"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("memo.txt")));
  }

  private String uploadTextFile(StorageDirectory storageDirectory) {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "memo.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
    FileUploadDto uploadedFile = storageService.uploadFiles(List.of(file), storageDirectory).get(0);
    return uploadedFile.getStoredFileName().substring(storageDirectory.path().length() + 1);
  }
}
