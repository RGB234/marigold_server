package com.sns.marigold.storage.service;

import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.exception.StorageException;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

  private final S3Template s3Template;
  private final S3Presigner s3Presigner;
  private final S3Properties s3Properties;

  public ImageUploadDto uploadFile(MultipartFile file) {
    FileUploadDto uploadedFile = uploadFileWithMetadata(file);
    return ImageUploadDto.builder().storedFileName(uploadedFile.getStoredFileName())
        .originalFileName(uploadedFile.getOriginalFileName()).build();
  }


  public List<ImageUploadDto> uploadImagesToS3(List<MultipartFile> images) {
    if (images == null || images.isEmpty()) {
      return Collections.emptyList();
    }
    List<ImageUploadDto> result = new ArrayList<>();
    try {
      for (MultipartFile image : images) {
        // uploadFile 내부에서 실패 시 예외를 던지도록 설계 권장
        result.add(this.uploadFile(image));
      }
    } catch (StorageException e) {
      // 업로드 중간에 실패하면, 이미 올라간 파일들 삭제 후 예외 발생
      this.deleteUploadedImagesFromS3(result);
      throw e;
    }
    return result;
  }

  public List<FileUploadDto> uploadFilesToS3(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      return Collections.emptyList();
    }
    List<FileUploadDto> result = new ArrayList<>();
    try {
      for (MultipartFile file : files) {
        result.add(this.uploadFileWithMetadata(file));
      }
    } catch (StorageException e) {
      this.deleteUploadedFilesFromS3(result);
      throw e;
    }
    return result;
  }

  public void deleteFile(String storedFileName) {
    Objects.requireNonNull(storedFileName, "storedFileName must not be null");
    String safeBucketName = bucketName();
    // 파일이 없으면 S3가 알아서 무시
    // 네트워크, 권한 문제 발생 가능. 사용자 경험을 고려하여 해당 예외는 로그만 남기고 그냥 무시 (고아 파일은 운영단에서 주기적으로 정리하도록 설계)
    try {
      s3Template.deleteObject(safeBucketName, storedFileName);
    } catch (Exception e) {
      // 파일을 못 지웠다고 해서 비즈니스 로직 전체를 실패로 만들지 않음
      log.warn("event=s3_delete_failed storedFileName={}", storedFileName, e);
    }
  }

  public void deleteUploadedImagesFromS3(List<ImageUploadDto> images) {
    for (ImageUploadDto dto : images) {
      this.deleteFile(dto.getStoredFileName());
    }
  }

  public void deleteUploadedImagesFromS3ByStoredFileNames(List<String> storedFileNames) {
    for (String storedFileName : storedFileNames) {
      this.deleteFile(storedFileName);
    }
  }

  public void deleteUploadedFilesFromS3(List<FileUploadDto> files) {
    for (FileUploadDto dto : files) {
      this.deleteFile(dto.getStoredFileName());
    }
  }

  /**
   * S3 조회용 Presigned URL 생성. DTO에서 fileName이 null인 경우에도 사용하므로 예외를 던지지 않고 그대로 null을 반환한다.
   *
   * @param storedFileName 저장된 파일명 (Key)
   * @return 접근 가능한 URL
   */
  public String getPresignedViewUrlOrNull(String storedFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      return null;
    }
    GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(bucketName())
        .key(storedFileName).build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(60)) // 1시간 유효
        .getObjectRequest(objectRequest).build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
  }

  /**
   * S3 다운로드용 Presigned URL 생성.
   *
   * @param storedFileName 저장된 파일명 (Key)
   * @return 접근 가능한 URL
   */
  public String getPresignedDownloadUrl(String storedFileName, String originalFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      throw StorageException.forFileNotFound();
    }
    String safeFileName = sanitizeContentDispositionFileName(originalFileName);

    String contentDisposition = ContentDisposition.attachment()
        .filename(safeFileName, StandardCharsets.UTF_8)
        .build()
        .toString();

    GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(bucketName())
        .key(storedFileName).responseContentDisposition(contentDisposition).build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(10)).getObjectRequest(objectRequest).build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
  }

  public void validateRealImageFiles(List<MultipartFile> files) {
    Tika tika = new Tika();
    // 파일의 실제 InputStream을 읽어 MIME 타입을 추론
    for (MultipartFile file : files) {
      try (InputStream inputStream = file.getInputStream()) {
        String detectedType = tika.detect(inputStream);
        if (!ValidationPolicy.Image.ALLOWED_MIME_TYPES.contains(detectedType)) {
          throw StorageException.forInvalidMimeType(file.getOriginalFilename(), detectedType);
        }
      } catch (IOException e) {
        throw StorageException.forFileReadFailed(file.getOriginalFilename(), e);
      }
    }
  }

  private FileUploadDto uploadFileWithMetadata(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw StorageException.forEmptyFile();
    }

    String originalFilename = file.getOriginalFilename();
    String key = Objects.requireNonNull(
        generateStoredFileName(originalFilename)); // Random UUID + 파일 확장자
    String contentType =
        file.getContentType() != null ? file.getContentType() : "application/octet-stream";

    try (InputStream inputStream = file.getInputStream()) {
      s3Template.upload(bucketName(), key, inputStream,
          ObjectMetadata.builder().contentType(contentType).contentLength(file.getSize()).build());
    } catch (IOException e) {
      log.error("File read failed", e);
      throw StorageException.forFileReadFailed(originalFilename, e);
    } catch (S3Exception e) {
      log.error("S3 upload failed", e);
      throw StorageException.forFileUploadFailed(e);
    }

    return FileUploadDto.builder().storedFileName(key).originalFileName(originalFilename)
        .contentType(contentType).fileSize(file.getSize()).build();
  }

  /*
    Content-disposition 헤더에 입력할 파일명을 sanitize
   */
  private String sanitizeContentDispositionFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "download";
    }
    return fileName.replace("\\", "_")
        .replace("\"", "_")
        .replace("\r", "_")
        .replace("\n", "_");
  }

  private String bucketName() {
    return Objects.requireNonNull(s3Properties.bucket(), "bucketName must not be null");
  }

  private String generateStoredFileName(String fileName) {
    return UUID.randomUUID().toString().concat(getFileExtension(fileName));
  }

  private String getFileExtension(String fileName) {
    int extensionStart = fileName == null ? -1 : fileName.lastIndexOf(".");
    if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
      throw StorageException.forMissingFileExtension(fileName);
    }
    return fileName.substring(extensionStart);
  }
}
