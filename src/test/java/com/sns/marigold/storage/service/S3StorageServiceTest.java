package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.exception.StorageException;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

  private static final byte[] PNG_BYTES =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

  @Mock private S3Template s3Template;

  @Mock private S3Presigner s3Presigner;

  private S3StorageService storageService;

  @BeforeEach
  void setUp() {
    storageService = new S3StorageService(s3Template, s3Presigner, new S3Properties("test-bucket"));
  }

  @Test
  @DisplayName("파일 업로드 성공 시 ImageUploadDto를 반환한다")
  void uploadImage_Success() {
    MockMultipartFile mockFile =
        new MockMultipartFile("file", "test-image.png", "text/plain", PNG_BYTES);

    given(
            s3Template.upload(
                eq("test-bucket"), any(String.class), any(ByteArrayInputStream.class), any()))
        .willReturn(null);

    ImageUploadDto result = storageService.uploadImage(mockFile, StorageDirectory.ADOPTION_POST);

    assertThat(result.getOriginalFileName()).isEqualTo("test-image.png");
    assertThat(result.getStoredFileName()).startsWith("adoption/post/");
    assertThat(result.getStoredFileName()).endsWith(".png");

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
    verify(s3Template, times(1))
        .upload(eq("test-bucket"), keyCaptor.capture(), any(), metadataCaptor.capture());
    assertThat(keyCaptor.getValue()).isEqualTo(result.getStoredFileName());
    assertThat(metadataCaptor.getValue()).extracting("contentType").isEqualTo("image/png");
  }

  @Test
  @DisplayName("빈 파일 업로드 시 StorageException이 발생한다")
  void uploadImage_EmptyFile() {
    MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

    assertThatThrownBy(() -> storageService.uploadImage(emptyFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("확장자가 없는 파일 업로드 시 StorageException이 발생한다")
  void uploadImage_NoExtension() {
    MockMultipartFile noExtensionFile =
        new MockMultipartFile("file", "test-image", "image/png", PNG_BYTES);

    assertThatThrownBy(
            () -> storageService.uploadImage(noExtensionFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("실제 이미지가 아닌 파일은 업로드하지 않는다")
  void uploadImage_InvalidMimeType() {
    MockMultipartFile invalidFile =
        new MockMultipartFile("file", "not-image.png", "image/png", "plain text".getBytes());

    assertThatThrownBy(
            () -> storageService.uploadImage(invalidFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
    verify(s3Template, never()).upload(any(), any(), any(), any());
  }

  @Test
  @DisplayName("지원하지 않는 이미지 확장자는 업로드하지 않는다")
  void uploadImage_UnsupportedExtension() {
    MockMultipartFile gifFile =
        new MockMultipartFile("file", "test-image.gif", "image/gif", PNG_BYTES);

    assertThatThrownBy(() -> storageService.uploadImage(gifFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
    verify(s3Template, never()).upload(any(), any(), any(), any());
  }

  @Test
  @DisplayName("확장자와 실제 MIME이 일치하지 않으면 업로드하지 않는다")
  void uploadImage_MismatchedExtensionAndMimeType() {
    MockMultipartFile mismatchedFile =
        new MockMultipartFile("file", "test-image.jpg", "image/jpeg", PNG_BYTES);

    assertThatThrownBy(
            () -> storageService.uploadImage(mismatchedFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
    verify(s3Template, never()).upload(any(), any(), any(), any());
  }

  @Test
  @DisplayName("이미지 크기 제한을 초과하면 업로드하지 않는다")
  void uploadImage_SizeExceeded() {
    byte[] oversizedImage = new byte[(int) ValidationPolicy.Image.MAX_SIZE_BYTES + 1];
    MockMultipartFile file =
        new MockMultipartFile("file", "large.png", "image/png", oversizedImage);

    assertThatThrownBy(() -> storageService.uploadImage(file, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
    verify(s3Template, never()).upload(any(), any(), any(), any());
  }

  @Test
  @DisplayName("복수 이미지 중 하나라도 잘못되면 업로드를 시작하지 않는다")
  void uploadImages_ValidatesAllFilesBeforeUpload() {
    MockMultipartFile validFile =
        new MockMultipartFile("images", "valid.png", "image/png", PNG_BYTES);
    MockMultipartFile invalidFile =
        new MockMultipartFile("images", "invalid.png", "image/png", "plain text".getBytes());

    assertThatThrownBy(
            () ->
                storageService.uploadImages(
                    List.of(validFile, invalidFile), StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
    verify(s3Template, never()).upload(any(), any(), any(), any());
  }

  @Test
  @DisplayName("이미지 읽기 실패는 서버 파일 읽기 오류로 유지한다")
  void uploadImage_ReadFailed() {
    MultipartFile unreadableFile =
        new MockMultipartFile("file", "test-image.png", "image/png", PNG_BYTES) {
          @Override
          public InputStream getInputStream() throws IOException {
            throw new IOException("cannot read");
          }
        };

    assertThatThrownBy(
            () -> storageService.uploadImage(unreadableFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOfSatisfying(
            StorageException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_READ_FAILED));
    verify(s3Template, never()).upload(any(), any(), any(), any());
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
}
