package com.sns.marigold.storage.service;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Template;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.exception.StorageException;

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

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Value("${spring.cloud.aws.region.static}")
  private String region;

  public ImageUploadDto uploadFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw StorageException.forFileInvalid();
    }

    String originalFilename = file.getOriginalFilename();
    String key = createFileName(originalFilename); // Random UUID + 파일 확장자

    try (InputStream inputStream = file.getInputStream()) {
      s3Template.upload(bucketName, key, inputStream,
          ObjectMetadata.builder().contentType(file.getContentType()).contentLength(file.getSize()).build());
    } catch (IOException | S3Exception e) {
      log.error("S3 upload failed", e);
      throw StorageException.forFileUploadFailed(e);
    }

    return ImageUploadDto.builder()
        .storedFileName(key)
        .originalFileName(originalFilename)
        .build();
  }

  public void deleteFile(String storedFileName) {
    Objects.requireNonNull(storedFileName, "storedFileName must not be null");
    String safeBucketName = Objects.requireNonNull(bucketName, "bucketName must not be null");
    // 파일이 없으면 S3가 알아서 무시
    // 네트워크, 권한 문제 발생 가능. 해당 예외는 로그만 남기고 그냥 무시 (고아 파일은 운영단에서 정리한다고 가정)
    try {
      s3Template.deleteObject(safeBucketName, storedFileName);
    } catch (Exception e) {
      // 파일을 못 지웠다고 해서 비즈니스 로직 전체를 실패로 만들지 않음
      log.error("S3 파일 삭제 실패 (나중에 배치로 지워야 함): {}", storedFileName, e);
    }
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
      this.deleteUploadedImagesFromS3(
          result);
      throw StorageException.forFileUploadFailed(e.getCause());
    }
    return result;
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

  /**
   * S3 다운로드/조회용 Presigned URL 생성 (비공개 버킷 접근용)
   *
   * @param storedFileName 저장된 파일명 (Key)
   * @return 접근 가능한 URL
   */
  public String getPresignedGetUrl(String storedFileName) {
    if (storedFileName == null || storedFileName.isEmpty()) {
      return null;
    }
    String safeBucketName = Objects.requireNonNull(bucketName, "bucketName must not be null");

    GetObjectRequest objectRequest = GetObjectRequest.builder()
        .bucket(safeBucketName)
        .key(storedFileName)
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(60)) // 1시간 유효
        .getObjectRequest(objectRequest)
        .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
    // return Map.of("url", presignedRequest.url().toString(), "filename", storedFileName);
  }

  private String createFileName(String fileName) {
    return UUID.randomUUID().toString().concat(getFileExtension(fileName));
  }

  private String getFileExtension(String fileName) {
    if (fileName.lastIndexOf(".") == -1) {
      throw StorageException.forFileInvalid(fileName);
    }
    return fileName.substring(fileName.lastIndexOf("."));
  }

  public void validateRealImageFiles(List<MultipartFile> files) {
    Tika tika = new Tika();
    // 파일의 실제 InputStream을 읽어 MIME 타입을 추론
    for (MultipartFile file : files) {
      try (InputStream inputStream = file.getInputStream()) {
        String detectType = tika.detect(inputStream);
        if (!detectType.startsWith("image/")) {
          throw StorageException.forFileInvalid(file.getOriginalFilename());
        }
      } catch (IOException e) {
        throw StorageException.forFileUploadFailed(e.getMessage(), e);
      }
    }
  }
}