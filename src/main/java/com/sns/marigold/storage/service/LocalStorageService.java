package com.sns.marigold.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.exception.StorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService extends AbstractStorageService {

  private static final String GET = "GET";

  private final LocalStorageProperties localStorageProperties;
  private final LocalStorageUrlSigner localStorageUrlSigner;

  @Override
  protected FileUploadDto uploadFileWithMetadata(
      MultipartFile file, StorageDirectory storageDirectory, String contentTypeOverride) {
    StorageFileInfo fileInfo = prepareFile(file, storageDirectory, contentTypeOverride);
    Path targetPath = resolveStoredFile(fileInfo.storedFileName());

    try (InputStream inputStream = file.getInputStream()) {
      Files.createDirectories(targetPath.getParent());
      Files.copy(inputStream, targetPath);
    } catch (IOException e) {
      deletePartialFile(targetPath);
      log.error("Local storage upload failed", e);
      throw StorageException.forFileUploadFailed(e);
    }

    return FileUploadDto.builder()
        .storedFileName(fileInfo.storedFileName())
        .originalFileName(fileInfo.originalFileName())
        .contentType(fileInfo.contentType())
        .fileSize(fileInfo.fileSize())
        .build();
  }

  @Override
  protected void deleteStoredFile(String storedFileName) throws IOException {
    Files.deleteIfExists(resolveStoredFile(storedFileName));
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

    Path rootPath = storageRoot();
    Path filePath = rootPath.resolve(storedFileName).normalize();
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
