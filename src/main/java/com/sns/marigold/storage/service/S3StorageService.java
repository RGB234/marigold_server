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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService extends AbstractStorageService {

  private final S3Template s3Template;
  private final S3Presigner s3Presigner;
  private final S3Client s3Client;
  private final S3Properties s3Properties;

  @Override
  protected FileUploadDto uploadFileWithMetadata(
      MultipartFile file, StorageDirectory storageDirectory, String contentTypeOverride) {
    StorageFileInfo fileInfo = prepareFile(file, storageDirectory, contentTypeOverride);

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
        GetObjectRequest.builder().bucket(bucketName()).key(storedFileName).build();

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

  @Override
  public String createUploadUrl(
      String storedFileName, String contentType, long size, Duration ttl) {
    validateStoredFileName(storedFileName);
    // Content-Type과 Content-Length를 서명에 넣어, 신고와 다른 파일은 S3가 거부하게 한다
    PutObjectRequest objectRequest =
        PutObjectRequest.builder()
            .bucket(bucketName())
            .key(storedFileName)
            .contentType(contentType)
            .contentLength(size)
            .build();

    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .putObjectRequest(objectRequest)
            .build();

    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }

  @Override
  public StoredFileHead readFileHead(String storedFileName, int maxBytes) {
    validateStoredFileName(storedFileName);
    try {
      long size =
          s3Client
              .headObject(
                  HeadObjectRequest.builder().bucket(bucketName()).key(storedFileName).build())
              .contentLength();
      if (size == 0) {
        return new StoredFileHead(0, new byte[0]);
      }
      byte[] head =
          s3Client
              .getObjectAsBytes(
                  GetObjectRequest.builder()
                      .bucket(bucketName())
                      .key(storedFileName)
                      .range("bytes=0-" + (Math.min(size, maxBytes) - 1))
                      .build())
              .asByteArray();
      return new StoredFileHead(size, head);
    } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
      if (e.statusCode() == 404) {
        throw StorageException.forUploadIncomplete(storedFileName);
      }
      throw StorageException.forFileReadFailed(storedFileName, e);
    } catch (SdkException e) {
      throw StorageException.forFileReadFailed(storedFileName, e);
    }
  }

  private String bucketName() {
    return Objects.requireNonNull(s3Properties.bucket(), "bucketName must not be null");
  }
}
