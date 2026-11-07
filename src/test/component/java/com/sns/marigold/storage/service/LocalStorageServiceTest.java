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
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.storage.exception.StorageException;

class LocalStorageServiceTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("PUT으로 받은 로컬 파일은 확정 후 조회할 수 있다")
  void storeUploadedFileAndReadFile() throws Exception {
    LocalStorageService storageService = localStorageService();
    String storedFileName = "chat/attachment/11111111-1111-1111-1111-111111111111.txt";
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

    storageService.storeUploadedFile(
        storedFileName, new ByteArrayInputStream(content), content.length);
    assertThat(tempDir.resolve(StorageDirectory.pendingKey(storedFileName))).isRegularFile();
    assertThatThrownBy(() -> storageService.readFile(storedFileName))
        .isInstanceOf(StorageException.class);

    storageService.promoteUpload(storedFileName, null);
    LocalStorageService.LocalStoredFile storedFile = storageService.readFile(storedFileName);

    assertThat(tempDir.resolve(storedFileName)).isRegularFile();
    assertThat(tempDir.resolve(StorageDirectory.pendingKey(storedFileName))).doesNotExist();
    assertThat(storedFile.contentType()).isEqualTo("text/plain");
    assertThat(storedFile.resource().getContentAsString(StandardCharsets.UTF_8)).isEqualTo("hello");
  }

  @Test
  @DisplayName("올리지 않은 파일은 확정할 수 없다")
  void promoteUpload_Missing() {
    LocalStorageService storageService = localStorageService();

    assertThatThrownBy(
            () ->
                storageService.promoteUpload(
                    "chat/attachment/11111111-1111-1111-1111-111111111111.txt", null))
        .isInstanceOfSatisfying(
            StorageException.class,
            e -> assertThat(e.getErrorSpec()).isEqualTo(StorageError.UPLOAD_INCOMPLETE));
  }

  @Test
  @DisplayName("정식 경로로 옮긴 뒤에만 확정된 업로드로 본다")
  void isPromoted() throws Exception {
    LocalStorageService storageService = localStorageService();
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
    storageService.storeUploadedFile(
        storedFileName, new ByteArrayInputStream(content), content.length);

    assertThat(storageService.isPromoted(storedFileName)).isFalse();
    storageService.promoteUpload(storedFileName, null);
    assertThat(storageService.isPromoted(storedFileName)).isTrue();
  }

  @Test
  @DisplayName("등록되지 않은 업로드는 pending/과 정식 경로 파일을 모두 지운다")
  void discardUpload_DeletesBothPaths() throws Exception {
    LocalStorageService storageService = localStorageService();
    String promoted = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    String pending = "adoption/post/22222222-2222-2222-2222-222222222222.png";
    byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
    storageService.storeUploadedFile(promoted, new ByteArrayInputStream(content), content.length);
    storageService.promoteUpload(promoted, null);
    storageService.storeUploadedFile(pending, new ByteArrayInputStream(content), content.length);

    storageService.discardUploadBestEffort(promoted);
    storageService.discardUploadBestEffort(pending);

    assertThat(tempDir.resolve(promoted)).doesNotExist();
    assertThat(tempDir.resolve(StorageDirectory.pendingKey(pending))).doesNotExist();
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
