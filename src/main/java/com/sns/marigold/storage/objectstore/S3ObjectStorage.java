package com.sns.marigold.storage.objectstore;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;

import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.exception.StorageException;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3ObjectStorage extends AbstractObjectStorage {

  private final S3Template s3Template;
  private final S3Presigner s3Presigner;
  private final S3Client s3Client;
  private final S3Properties s3Properties;

  @Override
  protected void deleteStoredFile(String key) {
    s3Template.deleteObject(bucketName(), key);
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
            .key(StorageDirectory.pendingKey(storedFileName))
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
    String pendingKey = StorageDirectory.pendingKey(storedFileName);
    try {
      HeadObjectResponse metadata =
          s3Client.headObject(
              HeadObjectRequest.builder().bucket(bucketName()).key(pendingKey).build());
      long size = metadata.contentLength();
      String version = metadata.eTag();
      if (size == 0) {
        return new StoredFileHead(0, new byte[0], version);
      }
      // 업로드 URL이 살아 있는 동안 pending/ 파일을 바꿀 수 있어, 크기를 읽은 파일과 같은 버전만 읽는다
      byte[] head =
          s3Client
              .getObjectAsBytes(
                  GetObjectRequest.builder()
                      .bucket(bucketName())
                      .key(pendingKey)
                      .ifMatch(version)
                      .range("bytes=0-" + (Math.min(size, maxBytes) - 1))
                      .build())
              .asByteArray();
      return new StoredFileHead(size, head, version);
    } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
      if (e.statusCode() == 404 || e.statusCode() == 412) {
        throw StorageException.forUploadIncomplete(storedFileName);
      }
      throw StorageException.forFileReadFailed(storedFileName, e);
    } catch (SdkException e) {
      throw StorageException.forFileReadFailed(storedFileName, e);
    }
  }

  @Override
  public void promoteUpload(String storedFileName, String version) {
    validateStoredFileName(storedFileName);
    String pendingKey = StorageDirectory.pendingKey(storedFileName);
    try {
      // 검사 뒤 pending/ 파일이 바뀌었으면 S3가 412로 거부한다
      s3Client.copyObject(
          CopyObjectRequest.builder()
              .sourceBucket(bucketName())
              .sourceKey(pendingKey)
              .copySourceIfMatch(version)
              .destinationBucket(bucketName())
              .destinationKey(storedFileName)
              .build());
    } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
      if (e.statusCode() == 404 || e.statusCode() == 412) {
        throw StorageException.forUploadIncomplete(storedFileName);
      }
      throw StorageException.forFileUploadFailed(e);
    } catch (SdkException e) {
      throw StorageException.forFileUploadFailed(e);
    }
    // 남아도 pending/ Lifecycle 규칙이 지운다
    try {
      deleteStoredFile(pendingKey);
    } catch (RuntimeException e) {
      log.warn("event=storage_pending_delete_failed storedFileName={}", storedFileName, e);
    }
  }

  // 없는 키에 404를 받으려면 s3:ListBucket 권한이 필요하다. 없으면 S3가 403을 돌려줘 확정이 모두 실패한다
  @Override
  public boolean isPromoted(String storedFileName) {
    validateStoredFileName(storedFileName);
    try {
      s3Client.headObject(
          HeadObjectRequest.builder().bucket(bucketName()).key(storedFileName).build());
      return true;
    } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
      if (e.statusCode() == 404) {
        return false;
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
