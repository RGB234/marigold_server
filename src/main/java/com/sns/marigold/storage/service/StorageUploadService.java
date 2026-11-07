package com.sns.marigold.storage.service;

import java.time.LocalDateTime;
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
import com.sns.marigold.storage.dto.UploadToken;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.objectstore.ObjectStorage;
import com.sns.marigold.storage.objectstore.StoredFileHead;
import com.sns.marigold.storage.repository.StorageFileRepository;
import com.sns.marigold.storage.service.UploadTokenCodec.UploadClaims;

/**
 * 클라이언트 직접 업로드의 발급과 확정.
 *
 * <p>발급하면 파일 행이 PENDING으로 생기고 파일은 pending/ 아래에 올라간다. 확정{@link #confirm}은 도메인 트랜잭션보다 먼저 호출해 파일을 정식
 * 키로 옮기고, 도메인 트랜잭션이 {@link StorageFileService#attach}로 행을 ATTACHED로 바꾼다. 어느 단계에서 실패해도 행은 PENDING으로
 * 남고 고아 정리가 만료된 PENDING의 파일을 지운다.
 */
@Service
public class StorageUploadService {

  // Tika가 형식 감지에 읽는 최대 길이와 맞춘다
  static final int DETECTION_BYTES = 64 * 1024;
  private static final Tika TIKA = new Tika();

  private final ObjectStorage objectStorage;
  private final UploadTokenCodec uploadTokenCodec;
  private final StorageFileRepository storageFileRepository;
  private final StorageUploadProperties properties;

  /** 유효시간 설정이 정합성 전제를 깨면 앱 기동을 거부한다. */
  public StorageUploadService(
      ObjectStorage objectStorage,
      UploadTokenCodec uploadTokenCodec,
      StorageFileRepository storageFileRepository,
      StorageUploadProperties properties,
      StorageCleanupProperties cleanupProperties) {
    // 토큰이 살아 있는 동안 고아 정리가 PENDING을 만료시키면, 확정한 파일이 지워진 뒤 그 파일을 가리키는 행이 커밋될 수 있다
    if (properties.tokenTtl().compareTo(cleanupProperties.minimumAge()) >= 0) {
      throw new IllegalStateException(
          "upload token ttl must be shorter than storage cleanup minimum age: tokenTtl="
              + properties.tokenTtl()
              + ", minimumAge="
              + cleanupProperties.minimumAge());
    }
    // URL이 토큰보다 오래 살면, 토큰이 만료돼 확정할 수 없는 파일을 계속 올릴 수 있다
    if (properties.urlTtl().compareTo(properties.tokenTtl()) > 0) {
      throw new IllegalStateException(
          "upload url ttl must not exceed upload token ttl: urlTtl="
              + properties.urlTtl()
              + ", tokenTtl="
              + properties.tokenTtl());
    }
    this.objectStorage = objectStorage;
    this.uploadTokenCodec = uploadTokenCodec;
    this.storageFileRepository = storageFileRepository;
    this.properties = properties;
  }

  /**
   * 파일마다 업로드 URL과 토큰을 발급하고 PENDING 행을 저장한다. 파일은 아직 올라오지 않았으므로 이름과 신고한 크기만 검사한다.
   *
   * @return 요청한 파일 순서와 같은 발급 결과
   */
  public List<UploadIssueResponseDto> issue(long userId, UploadIssueRequestDto request) {
    UploadPurpose purpose = request.getPurpose();
    List<UploadIssueRequestDto.FileItem> files = request.getFiles();
    if (files.size() > purpose.maxCount()) {
      throw StorageException.forFileCountExceeded(files.size(), purpose.maxCount());
    }
    // 정책 위반이 하나라도 있으면 행을 남기지 않도록 모두 검사한 뒤 저장한다
    List<IssuedUpload> issued =
        files.stream().map(file -> issueOne(userId, purpose, file)).toList();
    storageFileRepository.saveAll(issued.stream().map(IssuedUpload::file).toList());
    return issued.stream().map(IssuedUpload::response).toList();
  }

  /**
   * 토큰을 검증하고 실제 업로드된 파일의 크기와 형식을 검사한 뒤 정식 키로 옮긴다. 실패해도 올라간 파일은 지우지 않고 고아 정리에 맡긴다.
   *
   * <p>이미 정식 키에 있는 파일은 확정을 마친 것으로 보고 건너뛴다. 그래서 확정 뒤 저장이 실패한 요청을 토큰 유효시간 안에 같은 토큰으로 다시 보낼 수 있다. 이미
   * 연결된 토큰이면 {@link StorageFileService#attach}가 거부한다.
   *
   * @return 요청한 토큰 순서와 같은 확정 결과
   */
  public List<FileUploadDto> confirm(
      long userId, UploadPurpose purpose, List<UploadToken> uploadTokens) {
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
    // 정식 키를 먼저 확인해, 업로드 URL로 pending/에 다시 올린 파일이 확정된 파일을 덮어쓰지 못하게 한다
    List<UploadClaims> unpromoted =
        claimsList.stream()
            .filter(claims -> !objectStorage.isPromoted(claims.storedFileName()))
            .toList();
    // 하나라도 검사에 실패하면 아무것도 옮기지 않는다
    List<InspectedUpload> inspected = unpromoted.stream().map(this::inspectUploadedFile).toList();
    // pending/ 에 저장된 파일을 옮긴다.
    inspected.forEach(file -> objectStorage.promoteUpload(file.storedFileName(), file.version()));
    return claimsList.stream().map(this::toFileUploadDto).toList();
  }

  /** 파일 하나의 발급 결과와 PENDING 행을 만든다. 신고한 크기와 이름의 확장자만 보는 사전 검사이고, 실제 내용은 {@link #confirm}이 검사한다. */
  private IssuedUpload issueOne(
      long userId, UploadPurpose purpose, UploadIssueRequestDto.FileItem file) {
    String fileName = file.getFileName();
    if (file.getSize() > purpose.maxSizeBytes()) {
      throw StorageException.forFileSizeExceeded(fileName, file.getSize(), purpose.maxSizeBytes());
    }
    String extension = extensionOf(fileName);
    if (purpose.allowedMimeTypes(extension) == null) {
      throw StorageException.forUnsupportedFileExtension(fileName, extension);
    }

    // 경로 + 랜덤 UUID
    String storedFileName = purpose.directory().path() + "/" + UUID.randomUUID() + "." + extension;
    // 브라우저마다 File.type이 다르거나 비어 있어서, 서버가 확장자로 정한 값을 PUT 헤더로 쓰게 한다
    String contentType =
        MediaTypeFactory.getMediaType(fileName)
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

    StorageFile storageFile =
        StorageFile.builder()
            .storedFileName(storedFileName)
            .originalFileName(fileName)
            .contentType(contentType)
            .fileSize(file.getSize())
            .status(StorageFileStatus.PENDING)
            .statusChangedAt(LocalDateTime.now())
            .build();
    UploadIssueResponseDto response =
        UploadIssueResponseDto.builder()
            .uploadUrl(
                // pending/ + storedFileName
                objectStorage.createUploadUrl(
                    storedFileName, contentType, file.getSize(), properties.urlTtl()))
            .contentType(contentType)
            .uploadToken(
                new UploadToken(
                    uploadTokenCodec.issue(
                        storedFileName, userId, purpose, fileName, contentType, file.getSize())))
            .build();
    return new IssuedUpload(storageFile, response);
  }

  /** 서명·만료를 검증하고, 다른 사용자나 다른 용도로 발급한 토큰을 거부한다. */
  private UploadClaims verifyToken(long userId, UploadPurpose purpose, UploadToken token) {
    // JSON 배열의 null 원소는 null 토큰으로 들어온다. 형식 오류로 거부되게 null 그대로 넘긴다
    UploadClaims claims = uploadTokenCodec.verify(token == null ? null : token.value());
    if (claims.userId() != userId) {
      throw StorageException.forInvalidUploadToken("user mismatch");
    }
    if (claims.purpose() != purpose) {
      throw StorageException.forInvalidUploadToken("purpose mismatch");
    }
    return claims;
  }

  /**
   * pending/ 파일의 실제 크기가 토큰과 같고, 앞부분으로 감지한 형식이 확장자에 허용된 형식인지 검사한다. 이름만 바꾼 파일은 여기서 걸린다.
   *
   * @return 검사한 파일의 버전. 옮길 때 그 사이 바뀐 파일을 거부하는 데 쓴다.
   */
  private InspectedUpload inspectUploadedFile(UploadClaims claims) {
    StoredFileHead fileHead = objectStorage.readFileHead(claims.storedFileName(), DETECTION_BYTES);
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
    return new InspectedUpload(claims.storedFileName(), fileHead.version());
  }

  /** 서명된 토큰 값으로 결과를 만든다. 크기와 형식은 검사를 통과했으므로 토큰 값과 같다. */
  private FileUploadDto toFileUploadDto(UploadClaims claims) {
    return FileUploadDto.builder()
        .storedFileName(claims.storedFileName())
        .originalFileName(claims.originalFileName())
        .contentType(claims.contentType())
        .fileSize(claims.size())
        .build();
  }

  private record IssuedUpload(StorageFile file, UploadIssueResponseDto response) {}

  private record InspectedUpload(String storedFileName, String version) {}

  /** 마지막 점 뒤를 소문자로 반환한다. 확장자가 없으면 허용 목록과 비교할 수 없으므로 거부한다. */
  private String extensionOf(String fileName) {
    int extensionStart = fileName.lastIndexOf('.');
    if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
      throw StorageException.forMissingFileExtension(fileName);
    }
    return fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
  }
}
