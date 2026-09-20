package com.sns.marigold.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.exception.StorageException;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService extends AbstractStorageService {

  private final S3Template s3Template;
  private final S3Presigner s3Presigner;
  private final S3Properties s3Properties;

  @Override
  protected FileUploadDto uploadFileWithMetadata(
      MultipartFile file, StorageDirectory storageDirectory) {
    StorageFileInfo fileInfo = prepareFile(file, storageDirectory);

    try (InputStream inputStream = file.getInputStream()) {
      s3Template.upload(
          bucketName(),
          fileInfo.storedFileName(),
          inputStream,
          ObjectMetadata.builder()
              .contentType(fileInfo.contentType())
              .contentLength(fileInfo.fileSize())
              .build());
    } catch (IOException e) {
      log.error("File read failed", e);
      throw StorageException.forFileReadFailed(fileInfo.originalFileName(), e);
    } catch (S3Exception e) {
      log.error("S3 upload failed", e);
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
  protected void deleteStoredFile(String storedFileName) {
    s3Template.deleteObject(bucketName(), storedFileName);
  }

  @Override
  public String getViewUrlOrNull(String storedFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      return null;
    }
    validateStoredFileName(storedFileName);
    GetObjectRequest objectRequest =
        GetObjectRequest.builder()
            .bucket(bucketName())
            .key(storedFileName)
            .build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(60))
            .getObjectRequest(objectRequest)
            .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
  }

  @Override
  public String getDownloadUrl(String storedFileName, String originalFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      throw StorageException.forFileNotFound();
    }
    validateStoredFileName(storedFileName);
    String contentDisposition =
        ContentDisposition.attachment()
            .filename(sanitizeContentDispositionFileName(originalFileName), StandardCharsets.UTF_8)
            .build()
            .toString();

    GetObjectRequest objectRequest =
        GetObjectRequest.builder()
            .bucket(bucketName())
            .key(storedFileName)
            .responseContentDisposition(contentDisposition)
            .build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(objectRequest)
            .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
  }

  private String bucketName() {
    return Objects.requireNonNull(s3Properties.bucket(), "bucketName must not be null");
  }
}
