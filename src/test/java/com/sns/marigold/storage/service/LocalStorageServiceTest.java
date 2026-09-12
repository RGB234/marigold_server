package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.exception.StorageException;

class LocalStorageServiceTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("로컬 파일 업로드 후 조회할 수 있다")
  void uploadFilesAndReadFile() throws Exception {
    LocalStorageService storageService = localStorageService();
    MockMultipartFile file =
        new MockMultipartFile("file", "memo.txt", "text/plain", "hello".getBytes());

    FileUploadDto uploadedFile =
        storageService.uploadFiles(List.of(file), StorageDirectory.CHAT_ATTACHMENT).get(0);
    LocalStorageService.LocalStoredFile storedFile =
        storageService.readFile(uploadedFile.getStoredFileName());

    assertThat(uploadedFile.getStoredFileName()).startsWith("chat/attachment/");
    assertThat(uploadedFile.getStoredFileName()).endsWith(".txt");
    assertThat(tempDir.resolve(uploadedFile.getStoredFileName())).isRegularFile();
    assertThat(storedFile.contentType()).isEqualTo("text/plain");
    assertThat(storedFile.resource().getContentAsString(StandardCharsets.UTF_8)).isEqualTo("hello");
  }

  @Test
  @DisplayName("로컬 조회 URL은 백엔드 storage API를 반환한다")
  void getViewUrl() {
    LocalStorageService storageService = localStorageService();

    String url =
        storageService.getViewUrlOrNull("adoption/post/11111111-1111-1111-1111-111111111111.png");

    assertThat(url)
        .isEqualTo(
            "http://localhost:8080/api/v1/storage/files/adoption/post/11111111-1111-1111-1111-111111111111.png");
  }

  @Test
  @DisplayName("허용되지 않는 저장 파일명은 조회하지 않는다")
  void readFile_InvalidStoredFileName() {
    LocalStorageService storageService = localStorageService();

    assertThatThrownBy(() -> storageService.readFile("../secret.txt"))
        .isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("도메인 경로 없는 저장 파일명은 조회하지 않는다")
  void readFile_FlatStoredFileName() {
    LocalStorageService storageService = localStorageService();

    assertThatThrownBy(() -> storageService.readFile("11111111-1111-1111-1111-111111111111.png"))
        .isInstanceOf(StorageException.class);
  }

  private LocalStorageService localStorageService() {
    return new LocalStorageService(
        new LocalStorageProperties(tempDir.toString(), "http://localhost:8080"));
  }
}
