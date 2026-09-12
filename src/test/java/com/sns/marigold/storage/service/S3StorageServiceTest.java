package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.exception.StorageException;

import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

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
        new MockMultipartFile(
            "file", "test-image.png", "image/png", "test image content".getBytes());

    given(
            s3Template.upload(
                eq("test-bucket"), any(String.class), any(ByteArrayInputStream.class), any()))
        .willReturn(null);

    ImageUploadDto result = storageService.uploadImage(mockFile, StorageDirectory.ADOPTION_POST);

    assertThat(result.getOriginalFileName()).isEqualTo("test-image.png");
    assertThat(result.getStoredFileName()).startsWith("adoption/post/");
    assertThat(result.getStoredFileName()).endsWith(".png");

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3Template, times(1)).upload(eq("test-bucket"), keyCaptor.capture(), any(), any());
    assertThat(keyCaptor.getValue()).isEqualTo(result.getStoredFileName());
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
        new MockMultipartFile("file", "test-image", "image/png", "test content".getBytes());

    assertThatThrownBy(
            () -> storageService.uploadImage(noExtensionFile, StorageDirectory.ADOPTION_POST))
        .isInstanceOf(StorageException.class);
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
