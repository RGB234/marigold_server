package com.sns.marigold.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.exception.StorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService extends AbstractStorageService {

  private static final String GET = "GET";
  private static final String PUT = "PUT";

  private final LocalStorageProperties localStorageProperties;
  private final LocalStorageUrlSigner localStorageUrlSigner;

  @Override
  protected void deleteStoredFile(String key) throws IOException {
    Files.deleteIfExists(resolveKey(key));
  }

  @Override
  public String getViewUrlOrNull(String storedFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      return null;
    }
    validateStoredFileName(storedFileName);
    String path = toViewPath(storedFileName);
    long expiresAt =
        localStorageUrlSigner.expiresAt(
            Duration.ofMinutes(localStorageProperties.viewUrlTtlMinutes()));
    return UriComponentsBuilder.fromUriString(publicBaseUrl())
        .path(path)
        .queryParam("expires", expiresAt)
        .queryParam("signature", localStorageUrlSigner.sign(GET, path, expiresAt, null))
        .build()
        .encode(StandardCharsets.UTF_8)
        .toUriString();
  }

  @Override
  public String getDownloadUrl(String storedFileName, String originalFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      throw StorageException.forFileNotFound();
    }
    validateStoredFileName(storedFileName);
    String sanitizedOriginalFileName = sanitizeContentDispositionFileName(originalFileName);
    String path = toDownloadPath(storedFileName);
    long expiresAt =
        localStorageUrlSigner.expiresAt(
            Duration.ofMinutes(localStorageProperties.downloadUrlTtlMinutes()));
    return UriComponentsBuilder.fromUriString(publicBaseUrl())
        .path(path)
        .queryParam("filename", sanitizedOriginalFileName)
        .queryParam("expires", expiresAt)
        .queryParam(
            "signature",
            localStorageUrlSigner.sign(GET, path, expiresAt, sanitizedOriginalFileName))
        .build()
        .encode(StandardCharsets.UTF_8)
        .toUriString();
  }

  // 로컬은 개발용이라 Content-Type과 크기를 서명에 넣지 않는다. 크기는 PUT 수신 시 용도별 상한으로, 형식은 확정 단계에서 검사한다.
  @Override
  public String createUploadUrl(
      String storedFileName, String contentType, long size, Duration ttl) {
    validateStoredFileName(storedFileName);
    String path = toViewPath(storedFileName);
    long expiresAt = localStorageUrlSigner.expiresAt(ttl);
    return UriComponentsBuilder.fromUriString(publicBaseUrl())
        .path(path)
        .queryParam("expires", expiresAt)
        .queryParam("signature", localStorageUrlSigner.sign(PUT, path, expiresAt, null))
        .build()
        .encode(StandardCharsets.UTF_8)
        .toUriString();
  }

  @Override
  public StoredFileHead readFileHead(String storedFileName, int maxBytes) {
    Path filePath = resolvePendingFile(storedFileName);
    if (!Files.isRegularFile(filePath)) {
      throw StorageException.forUploadIncomplete(storedFileName);
    }
    try (InputStream inputStream = Files.newInputStream(filePath)) {
      return new StoredFileHead(Files.size(filePath), inputStream.readNBytes(maxBytes));
    } catch (IOException e) {
      throw StorageException.forFileReadFailed(storedFileName, e);
    }
  }

  @Override
  public void promoteUpload(String storedFileName) {
    Path pendingPath = resolvePendingFile(storedFileName);
    Path targetPath = resolveStoredFile(storedFileName);
    try {
      Files.createDirectories(targetPath.getParent());
      Files.move(pendingPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (NoSuchFileException e) {
      throw StorageException.forUploadIncomplete(storedFileName);
    } catch (IOException e) {
      throw StorageException.forFileUploadFailed(e);
    }
  }

  // 업로드 URL은 정식 경로 그대로 쓰고, 파일은 확정 전까지 pending/ 아래에 둔다
  public void storeUploadedFile(
      String storedFileName, InputStream inputStream, long contentLength) {
    Path targetPath = resolvePendingFile(storedFileName);
    long maxSize =
        UploadPurpose.ofStoredFileName(storedFileName)
            .map(UploadPurpose::maxSizeBytes)
            .orElseThrow(StorageException::forFileNotFound);
    if (contentLength <= 0 || contentLength > maxSize) {
      throw StorageException.forFileSizeExceeded(storedFileName, contentLength, maxSize);
    }

    // 확정 뒤 같은 URL로 다시 올린 파일은 어떤 행도 가리키지 않아 정리되지 않는다
    if (Files.exists(resolveStoredFile(storedFileName))) {
      throw StorageException.forFileUploadFailed(new FileAlreadyExistsException(storedFileName));
    }

    try {
      Files.createDirectories(targetPath.getParent());
      // 같은 URL로 올린 파일을 덮어쓰지 못하게 새 파일로만 쓴다
      try (OutputStream outputStream =
          Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW)) {
        long copied = inputStream.transferTo(outputStream);
        if (copied != contentLength) {
          throw new IOException(
              "body size mismatch: expected=" + contentLength + ", actual=" + copied);
        }
      }
    } catch (FileAlreadyExistsException e) {
      throw StorageException.forFileUploadFailed(e);
    } catch (IOException e) {
      deletePartialFile(targetPath);
      log.error("Local storage upload failed", e);
      throw StorageException.forFileUploadFailed(e);
    }
  }

  public LocalStoredFile readFile(String storedFileName) {
    Path filePath = resolveStoredFile(storedFileName);
    if (!Files.isRegularFile(filePath)) {
      throw StorageException.forFileNotFound();
    }

    try {
      String contentType = Files.probeContentType(filePath);
      return new LocalStoredFile(
          new FileSystemResource(filePath),
          contentType != null ? contentType : "application/octet-stream",
          Files.size(filePath));
    } catch (IOException e) {
      throw StorageException.forFileReadFailed(storedFileName, e);
    }
  }

  private Path resolveStoredFile(String storedFileName) {
    validateStoredFileName(storedFileName);
    return resolveKey(storedFileName);
  }

  private Path resolvePendingFile(String storedFileName) {
    validateStoredFileName(storedFileName);
    return resolveKey(StorageDirectory.pendingKey(storedFileName));
  }

  private Path resolveKey(String key) {
    Path rootPath = storageRoot();
    Path filePath = rootPath.resolve(key).normalize();
    if (!filePath.startsWith(rootPath)) {
      throw StorageException.forFileNotFound();
    }
    return filePath;
  }

  private Path storageRoot() {
    return Path.of(
            Objects.requireNonNull(
                localStorageProperties.rootPath(), "local storage root path must not be null"))
        .toAbsolutePath()
        .normalize();
  }

  private String publicBaseUrl() {
    return Objects.requireNonNull(
        localStorageProperties.publicBaseUrl(), "local storage public base URL must not be null");
  }

  private String toViewPath(String storedFileName) {
    return UrlConstants.STORAGE_BASE + "/files/" + storedFileName;
  }

  private String toDownloadPath(String storedFileName) {
    return toViewPath(storedFileName) + "/download";
  }

  private void deletePartialFile(Path targetPath) {
    try {
      Files.deleteIfExists(targetPath);
    } catch (IOException e) {
      log.warn("event=local_storage_partial_delete_failed path={}", targetPath, e);
    }
  }

  public record LocalStoredFile(Resource resource, String contentType, long contentLength) {}
}
