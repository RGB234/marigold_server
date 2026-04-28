package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.exception.StorageException;
import io.awspring.cloud.s3.S3Template;
import java.io.ByteArrayInputStream;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

  @Mock private S3Template s3Template;

  @Mock private S3Presigner s3Presigner;

  @InjectMocks private S3Service s3Service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
    ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
  }

  @Test
  @DisplayName("파일 업로드 성공 시 ImageUploadDto를 반환한다")
  void uploadFile_Success() throws Exception {
    // given
    MockMultipartFile mockFile =
        new MockMultipartFile(
            "file", "test-image.png", "image/png", "test image content".getBytes());

    given(
            s3Template.upload(
                eq("test-bucket"), any(String.class), any(ByteArrayInputStream.class), any()))
        .willReturn(null); // S3Template의 upload는 S3Resource를 리턴하지만 여기선 필요 없음

    // when
    ImageUploadDto result = s3Service.uploadFile(mockFile);

    // then
    assertThat(result.getOriginalFileName()).isEqualTo("test-image.png");
    assertThat(result.getStoredFileName()).endsWith(".png");
    verify(s3Template, times(1)).upload(eq("test-bucket"), any(String.class), any(), any());
  }

  @Test
  @DisplayName("빈 파일 업로드 시 StorageException이 발생한다")
  void uploadFile_EmptyFile() {
    // given
    MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

    // when & then
    assertThatThrownBy(() -> s3Service.uploadFile(emptyFile)).isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("확장자가 없는 파일 업로드 시 StorageException이 발생한다")
  void uploadFile_NoExtension() {
    // given
    MockMultipartFile noExtensionFile =
        new MockMultipartFile(
            "file",
            "test-image", // 확장자 없음
            "image/png",
            "test content".getBytes());

    // when & then
    assertThatThrownBy(() -> s3Service.uploadFile(noExtensionFile))
        .isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("Presigned URL 발급 성공")
  void getPresignedGetObject_Success() throws Exception {
    // given
    String storedFileName = "uuid-name.png";
    URL fakeUrl = new URL("https://test-bucket.s3.amazonaws.com/" + storedFileName + "?...");

    PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
    given(presignedRequest.url()).willReturn(fakeUrl);

    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .willReturn(presignedRequest);

    // when
    String url = s3Service.getPresignedGetObject(storedFileName);

    // then
    assertThat(url).isEqualTo(fakeUrl.toString());

    ArgumentCaptor<GetObjectPresignRequest> captor =
        ArgumentCaptor.forClass(GetObjectPresignRequest.class);
    verify(s3Presigner, times(1)).presignGetObject(captor.capture());

    GetObjectPresignRequest capturedRequest = captor.getValue();
    assertThat(capturedRequest.getObjectRequest().key()).isEqualTo(storedFileName);
  }
}
