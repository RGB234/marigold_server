package com.sns.marigold.storage.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import com.sns.marigold.storage.cleanup.StorageCleanupProperties;
import com.sns.marigold.storage.config.StorageUploadProperties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.UploadIssueRequestDto;
import com.sns.marigold.storage.dto.UploadIssueResponseDto;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.UploadTokenCodec.UploadClaims;

/**
 * 클라이언트 직접 업로드의 발급과 확정.
 *
 * <p>확정은 도메인 트랜잭션보다 먼저 호출한다. 그래야 어느 단계에서 실패해도 결과가 "참조되지 않은 파일"에서 끝나고, 그 파일은 고아 정리가 지운다.
 */
@Service
public class StorageUploadService {

  // Tika가 형식 감지에 읽는 최대 길이와 맞춘다
  static final int DETECTION_BYTES = 64 * 1024;
  private static final Tika TIKA = new Tika();

  private final StorageService storageService;
  private final UploadTokenCodec uploadTokenCodec;
  private final StorageUploadProperties properties;

  public StorageUploadService(
      StorageService storageService,
      UploadTokenCodec uploadTokenCodec,
      StorageUploadProperties properties,
      StorageCleanupProperties cleanupProperties) {
    // 토큰이 살아 있는 동안 고아 정리가 확정 전 파일을 지우면, 커밋된 행이 없는 파일을 가리키게 된다
    if (properties.tokenTtl().compareTo(cleanupProperties.minimumAge()) >= 0) {
      throw new IllegalStateException(
          "upload token ttl must be shorter than storage cleanup minimum age: tokenTtl="
              + properties.tokenTtl()
              + ", minimumAge="
              + cleanupProperties.minimumAge());
    }
    if (properties.urlTtl().compareTo(properties.tokenTtl()) > 0) {
      throw new IllegalStateException(
          "upload url ttl must not exceed upload token ttl: urlTtl="
              + properties.urlTtl()
              + ", tokenTtl="
              + properties.tokenTtl());
    }
    this.storageService = storageService;
    this.uploadTokenCodec = uploadTokenCodec;
    this.properties = properties;
  }

  public List<UploadIssueResponseDto> issue(long userId, UploadIssueRequestDto request) {
    UploadPurpose purpose = request.getPurpose();
    List<UploadIssueRequestDto.FileItem> files = request.getFiles();
    if (files.size() > purpose.maxCount()) {
      throw StorageException.forFileCountExceeded(files.size(), purpose.maxCount());
    }
    return files.stream().map(file -> issueOne(userId, purpose, file)).toList();
  }

  /**
   * 토큰을 검증하고 실제 업로드된 파일의 크기와 형식을 검사한다. 실패해도 올라간 파일은 지우지 않고 고아 정리에 맡긴다.
   *
   * @return 요청한 토큰 순서와 같은 확정 결과
   */
  public List<FileUploadDto> confirm(
      long userId, UploadPurpose purpose, List<String> uploadTokens) {
    if (uploadTokens == null || uploadTokens.isEmpty()) {
      return List.of();
    }
    if (uploadTokens.size() > purpose.maxCount()) {
      throw StorageException.forFileCountExceeded(uploadTokens.size(), purpose.maxCount());
    }

    List<UploadClaims> claimsList =
        uploadTokens.stream().map(token -> verifyToken(userId, purpose, token)).toList();
    long distinctCount = claimsList.stream().map(UploadClaims::storedFileName).distinct().count();
    if (distinctCount != claimsList.size()) {
      throw StorageException.forInvalidUploadToken("duplicated");
    }
    return claimsList.stream().map(this::inspectUploadedFile).toList();
  }

  private UploadIssueResponseDto issueOne(
      long userId, UploadPurpose purpose, UploadIssueRequestDto.FileItem file) {
    String fileName = file.getFileName();
    if (file.getSize() > purpose.maxSizeBytes()) {
      throw StorageException.forFileSizeExceeded(fileName, file.getSize(), purpose.maxSizeBytes());
    }
    String extension = extensionOf(fileName);
    if (purpose.allowedMimeTypes(extension) == null) {
      throw StorageException.forUnsupportedFileExtension(fileName, extension);
    }

    String storedFileName = purpose.directory().path() + "/" + UUID.randomUUID() + "." + extension;
    // 브라우저마다 File.type이 다르거나 비어 있어서, 서버가 확장자로 정한 값을 PUT 헤더로 쓰게 한다
    String contentType =
        MediaTypeFactory.getMediaType(fileName)
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

    return UploadIssueResponseDto.builder()
        .uploadUrl(
            storageService.createUploadUrl(
                storedFileName, contentType, file.getSize(), properties.urlTtl()))
        .contentType(contentType)
        .uploadToken(
            uploadTokenCodec.issue(
                storedFileName, userId, purpose, fileName, contentType, file.getSize()))
        .build();
  }

  private UploadClaims verifyToken(long userId, UploadPurpose purpose, String token) {
    UploadClaims claims = uploadTokenCodec.verify(token);
    if (claims.userId() != userId) {
      throw StorageException.forInvalidUploadToken("user mismatch");
    }
    if (claims.purpose() != purpose) {
      throw StorageException.forInvalidUploadToken("purpose mismatch");
    }
    return claims;
  }

  private FileUploadDto inspectUploadedFile(UploadClaims claims) {
    StoredFileHead fileHead = storageService.readFileHead(claims.storedFileName(), DETECTION_BYTES);
    if (fileHead.size() != claims.size()) {
      throw StorageException.forUploadSizeMismatch(
          claims.storedFileName(), claims.size(), fileHead.size());
    }

    String extension = extensionOf(claims.storedFileName());
    List<String> allowedMimeTypes = claims.purpose().allowedMimeTypes(extension);
    String detectedMimeType = TIKA.detect(fileHead.head());
    if (allowedMimeTypes == null || !allowedMimeTypes.contains(detectedMimeType)) {
      throw StorageException.forInvalidMimeType(
          claims.originalFileName(), extension, detectedMimeType);
    }

    return FileUploadDto.builder()
        .storedFileName(claims.storedFileName())
        .originalFileName(claims.originalFileName())
        .contentType(claims.contentType())
        .fileSize(claims.size())
        .build();
  }

  private String extensionOf(String fileName) {
    int extensionStart = fileName.lastIndexOf('.');
    if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
      throw StorageException.forMissingFileExtension(fileName);
    }
    return fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
  }
}
