package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.storage.exception.StorageException;

import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

  private static final byte[] PNG_BYTES =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

  @Mock private S3Template s3Template;

  @Mock private S3Presigner s3Presigner;

  @Mock private S3Client s3Client;

  private S3StorageService storageService;

  @BeforeEach
  void setUp() {
    storageService =
        new S3StorageService(s3Template, s3Presigner, s3Client, new S3Properties("test-bucket"));
  }

  @Test
  @DisplayName("조회 URL 발급 성공")
  void getViewUrl_Success() throws Exception {
    String storedFileName = "chat/attachment/11111111-1111-1111-1111-111111111111.png";
    URL fakeUrl = new URL("https://test-bucket.s3.amazonaws.com/" + storedFileName + "?...");

    PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
    given(presignedRequest.url()).willReturn(fakeUrl);

    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .willReturn(presignedRequest);

    String url = storageService.getViewUrlOrNull(storedFileName);

    assertThat(url).isEqualTo(fakeUrl.toString());

    ArgumentCaptor<GetObjectPresignRequest> captor =
        ArgumentCaptor.forClass(GetObjectPresignRequest.class);
    verify(s3Presigner, times(1)).presignGetObject(captor.capture());

    GetObjectPresignRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getObjectRequest().key()).isEqualTo(storedFileName);
  }

  @Test
  @DisplayName("다운로드 URL 발급 시 저장 파일명이 없으면 StorageException이 발생한다")
  void getDownloadUrl_EmptyStoredFileName() {
    assertThatThrownBy(() -> storageService.getDownloadUrl(null, "original.txt"))
        .isInstanceOf(StorageException.class);

    assertThatThrownBy(() -> storageService.getDownloadUrl(" ", "original.txt"))
        .isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("업로드 URL은 Content-Type과 크기를 서명에 포함한다")
  void createUploadUrl_SignsContentTypeAndLength() throws Exception {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    URL fakeUrl = new URL("https://test-bucket.s3.amazonaws.com/" + storedFileName + "?...");
    PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
    given(presignedRequest.url()).willReturn(fakeUrl);
    given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
        .willReturn(presignedRequest);

    String url =
        storageService.createUploadUrl(storedFileName, "image/png", 100L, Duration.ofMinutes(15));

    assertThat(url).isEqualTo(fakeUrl.toString());
    ArgumentCaptor<PutObjectPresignRequest> captor =
        ArgumentCaptor.forClass(PutObjectPresignRequest.class);
    verify(s3Presigner).presignPutObject(captor.capture());
    assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
    assertThat(captor.getValue().putObjectRequest().key()).isEqualTo(storedFileName);
    assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/png");
    assertThat(captor.getValue().putObjectRequest().contentLength()).isEqualTo(100L);
  }

  @Test
  @DisplayName("파일 앞부분은 실제 크기 안에서 Range로 읽는다")
  void readFileHead_ReadsRange() {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    given(s3Client.headObject(any(HeadObjectRequest.class)))
        .willReturn(HeadObjectResponse.builder().contentLength((long) PNG_BYTES.length).build());
    given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
        .willReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), PNG_BYTES));

    StoredFileHead head = storageService.readFileHead(storedFileName, 64 * 1024);

    assertThat(head.size()).isEqualTo(PNG_BYTES.length);
    assertThat(head.head()).isEqualTo(PNG_BYTES);
    ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
    verify(s3Client).getObjectAsBytes(captor.capture());
    assertThat(captor.getValue().range()).isEqualTo("bytes=0-" + (PNG_BYTES.length - 1));
  }

  @Test
  @DisplayName("업로드되지 않은 파일을 읽으면 UPLOAD_INCOMPLETE")
  void readFileHead_Missing() {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.png";
    given(s3Client.headObject(any(HeadObjectRequest.class)))
        .willThrow(NoSuchKeyException.builder().statusCode(404).build());

    assertThatThrownBy(() -> storageService.readFileHead(storedFileName, 64 * 1024))
        .isInstanceOfSatisfying(
            StorageException.class,
            exception ->
                assertThat(exception.getErrorSpec()).isEqualTo(StorageError.UPLOAD_INCOMPLETE));
  }
}
