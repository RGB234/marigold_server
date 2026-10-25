package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.exception.StorageException;

class LocalStorageServiceTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("PUT으로 받은 로컬 파일을 조회할 수 있다")
  void storeUploadedFileAndReadFile() throws Exception {
    LocalStorageService storageService = localStorageService();
    String storedFileName = "chat/attachment/11111111-1111-1111-1111-111111111111.txt";
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

    storageService.storeUploadedFile(
        storedFileName, new ByteArrayInputStream(content), content.length);
    LocalStorageService.LocalStoredFile storedFile = storageService.readFile(storedFileName);

    assertThat(tempDir.resolve(storedFileName)).isRegularFile();
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
        .startsWith(
            "http://localhost:8080/api/v1/storage/files/adoption/post/11111111-1111-1111-1111-111111111111.png?");
    assertThat(URI.create(url).getQuery()).contains("expires=", "signature=");
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
    LocalStorageProperties properties =
        new LocalStorageProperties(
            tempDir.toString(), "http://localhost:8080", "test-signing-secret", 60, 10);
    return new LocalStorageService(properties, new LocalStorageUrlSigner(properties));
  }
}
