package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.storage.cleanup.StorageCleanupMode;
import com.sns.marigold.storage.cleanup.StorageCleanupProperties;
import com.sns.marigold.storage.config.StorageUploadProperties;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.UploadIssueRequestDto;
import com.sns.marigold.storage.dto.UploadIssueResponseDto;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.repository.StorageFileRepository;

@ExtendWith(MockitoExtension.class)
class StorageUploadServiceTest {

  private static final byte[] PNG_BYTES =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
  private static final long USER_ID = 7L;
  private static final StorageUploadProperties PROPERTIES =
      new StorageUploadProperties("test-secret", Duration.ofMinutes(15), Duration.ofHours(1));

  @Mock private StorageService storageService;
  @Mock private StorageFileRepository storageFileRepository;

  private UploadTokenCodec uploadTokenCodec;
  private StorageUploadService uploadService;

  @BeforeEach
  void setUp() {
    uploadTokenCodec = new UploadTokenCodec(PROPERTIES);
    uploadService =
        new StorageUploadService(
            storageService,
            uploadTokenCodec,
            storageFileRepository,
            PROPERTIES,
            cleanupWithMinimumAge(Duration.ofHours(24)));
  }

  @Test
  void constructor_TokenTtlNotShorterThanCleanupMinimumAge() {
    assertThatThrownBy(
            () ->
                new StorageUploadService(
                    storageService,
                    uploadTokenCodec,
                    storageFileRepository,
                    PROPERTIES,
                    cleanupWithMinimumAge(PROPERTIES.tokenTtl())))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void constructor_UrlTtlLongerThanTokenTtl() {
    StorageUploadProperties properties =
        new StorageUploadProperties("test-secret", Duration.ofHours(2), Duration.ofHours(1));

    assertThatThrownBy(
            () ->
                new StorageUploadService(
                    storageService,
                    uploadTokenCodec,
                    storageFileRepository,
                    properties,
                    cleanupWithMinimumAge(Duration.ofHours(24))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void issue_Success() {
    given(storageService.createUploadUrl(anyString(), anyString(), anyLong(), any()))
        .willReturn("https://upload.example/url");

    List<UploadIssueResponseDto> result =
        uploadService.issue(
            USER_ID, request(UploadPurpose.ADOPTION_POST_IMAGE, file("Cat.JPG", 100)));

    assertThat(result).hasSize(1);
    UploadIssueResponseDto issued = result.get(0);
    assertThat(issued.getUploadUrl()).isEqualTo("https://upload.example/url");
    assertThat(issued.getContentType()).isEqualTo("image/jpeg");

    UploadTokenCodec.UploadClaims claims = uploadTokenCodec.verify(issued.getUploadToken());
    assertThat(claims.storedFileName()).startsWith("adoption/post/").endsWith(".jpg");
    assertThat(StorageDirectory.isValidStoredFileName(claims.storedFileName())).isTrue();
    assertThat(claims.originalFileName()).isEqualTo("Cat.JPG");
    assertThat(claims.userId()).isEqualTo(USER_ID);
    verify(storageService)
        .createUploadUrl(claims.storedFileName(), "image/jpeg", 100L, PROPERTIES.urlTtl());
    verify(storageFileRepository)
        .saveAll(
            argThat(
                (List<StorageFile> files) ->
                    files.size() == 1
                        && files.get(0).getStoredFileName().equals(claims.storedFileName())
                        && files.get(0).getOriginalFileName().equals("Cat.JPG")
                        && files.get(0).getContentType().equals("image/jpeg")
                        && files.get(0).getFileSize() == 100L
                        && files.get(0).getStatus() == StorageFileStatus.PENDING));
  }

  @Test
  void issue_SavesNothingWhenAnyFileIsInvalid() {
    given(storageService.createUploadUrl(anyString(), anyString(), anyLong(), any()))
        .willReturn("https://upload.example/url");

    assertError(
        () ->
            uploadService.issue(
                USER_ID,
                request(
                    UploadPurpose.ADOPTION_POST_IMAGE,
                    file("cat.jpg", 100),
                    file("memo.pdf", 100))),
        StorageError.FILE_INVALID);
    verify(storageFileRepository, never()).saveAll(any());
  }

  @Test
  void issue_CountExceeded() {
    UploadIssueRequestDto.FileItem[] files =
        IntStream.range(0, 2)
            .mapToObj(i -> file("cat.png", 100))
            .toArray(UploadIssueRequestDto.FileItem[]::new);

    assertError(
        () -> uploadService.issue(USER_ID, request(UploadPurpose.USER_PROFILE_IMAGE, files)),
        StorageError.FILE_INVALID);
    verify(storageService, never()).createUploadUrl(anyString(), anyString(), anyLong(), any());
  }

  @Test
  void issue_SizeExceeded() {
    long maxSize = UploadPurpose.CHAT_ATTACHMENT.maxSizeBytes();

    assertError(
        () ->
            uploadService.issue(
                USER_ID, request(UploadPurpose.CHAT_ATTACHMENT, file("memo.pdf", maxSize + 1))),
        StorageError.FILE_INVALID);
  }

  @Test
  void issue_UnsupportedExtension() {
    assertError(
        () ->
            uploadService.issue(
                USER_ID, request(UploadPurpose.ADOPTION_POST_IMAGE, file("memo.pdf", 100))),
        StorageError.FILE_INVALID);
  }

  @Test
  void confirm_Success() {
    String token = tokenFor(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/a.png");
    givenStoredFile("adoption/post/a.png", PNG_BYTES);

    List<FileUploadDto> result =
        uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStoredFileName()).isEqualTo("adoption/post/a.png");
    assertThat(result.get(0).getOriginalFileName()).isEqualTo("original.png");
    assertThat(result.get(0).getContentType()).isEqualTo("image/png");
    assertThat(result.get(0).getFileSize()).isEqualTo(PNG_BYTES.length);
    verify(storageService).promoteUpload("adoption/post/a.png");
  }

  @Test
  void confirm_PromotesNothingWhenAnyFileFails() {
    String first = tokenFor(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/a.png");
    String second = tokenFor(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/b.png");
    givenStoredFile("adoption/post/a.png", PNG_BYTES);
    given(storageService.readFileHead("adoption/post/b.png", StorageUploadService.DETECTION_BYTES))
        .willThrow(StorageException.forUploadIncomplete("adoption/post/b.png"));

    assertError(
        () ->
            uploadService.confirm(
                USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(first, second)),
        StorageError.UPLOAD_INCOMPLETE);
    verify(storageService, never()).promoteUpload(anyString());
  }

  @Test
  void confirm_EmptyTokens() {
    assertThat(uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, null)).isEmpty();
    assertThat(uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of()))
        .isEmpty();
  }

  @Test
  void confirm_OtherUsersToken() {
    String token = tokenFor(USER_ID + 1, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/a.png");

    assertError(
        () -> uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token)),
        StorageError.UPLOAD_TOKEN_INVALID);
  }

  @Test
  void confirm_OtherPurposeToken() {
    String token = tokenFor(USER_ID, UploadPurpose.CHAT_ATTACHMENT, "chat/attachment/a.png");

    assertError(
        () -> uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token)),
        StorageError.UPLOAD_TOKEN_INVALID);
  }

  @Test
  void confirm_DuplicatedToken() {
    String token = tokenFor(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/a.png");

    assertError(
        () ->
            uploadService.confirm(
                USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token, token)),
        StorageError.UPLOAD_TOKEN_INVALID);
    verify(storageService, never())
        .readFileHead(anyString(), eq(StorageUploadService.DETECTION_BYTES));
  }

  @Test
  void confirm_CountExceeded() {
    String first = tokenFor(USER_ID, UploadPurpose.USER_PROFILE_IMAGE, "user/profile/a.png");
    String second = tokenFor(USER_ID, UploadPurpose.USER_PROFILE_IMAGE, "user/profile/b.png");

    assertError(
        () ->
            uploadService.confirm(
                USER_ID, UploadPurpose.USER_PROFILE_IMAGE, List.of(first, second)),
        StorageError.FILE_INVALID);
  }

  @Test
  void confirm_SizeMismatch() {
    String token = tokenFor(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/a.png");
    given(storageService.readFileHead("adoption/post/a.png", StorageUploadService.DETECTION_BYTES))
        .willReturn(new StoredFileHead(PNG_BYTES.length + 1L, PNG_BYTES));

    assertError(
        () -> uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token)),
        StorageError.UPLOAD_INCOMPLETE);
  }

  @Test
  void confirm_MagicBytesMismatch() {
    byte[] text = "not an image".getBytes(StandardCharsets.UTF_8);
    String token =
        uploadTokenCodec.issue(
            "adoption/post/a.png",
            USER_ID,
            UploadPurpose.ADOPTION_POST_IMAGE,
            "original.png",
            "image/png",
            text.length);
    givenStoredFile("adoption/post/a.png", text);

    assertError(
        () -> uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token)),
        StorageError.FILE_INVALID);
  }

  @Test
  void confirm_FileMissing() {
    String token = tokenFor(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, "adoption/post/a.png");
    given(storageService.readFileHead("adoption/post/a.png", StorageUploadService.DETECTION_BYTES))
        .willThrow(StorageException.forUploadIncomplete("adoption/post/a.png"));

    assertError(
        () -> uploadService.confirm(USER_ID, UploadPurpose.ADOPTION_POST_IMAGE, List.of(token)),
        StorageError.UPLOAD_INCOMPLETE);
  }

  private String tokenFor(long userId, UploadPurpose purpose, String storedFileName) {
    return uploadTokenCodec.issue(
        storedFileName, userId, purpose, "original.png", "image/png", PNG_BYTES.length);
  }

  private void givenStoredFile(String storedFileName, byte[] bytes) {
    given(storageService.readFileHead(storedFileName, StorageUploadService.DETECTION_BYTES))
        .willReturn(new StoredFileHead(bytes.length, bytes));
  }

  private static UploadIssueRequestDto request(
      UploadPurpose purpose, UploadIssueRequestDto.FileItem... files) {
    return UploadIssueRequestDto.builder().purpose(purpose).files(List.of(files)).build();
  }

  private static UploadIssueRequestDto.FileItem file(String fileName, long size) {
    return UploadIssueRequestDto.FileItem.builder().fileName(fileName).size(size).build();
  }

  private static StorageCleanupProperties cleanupWithMinimumAge(Duration minimumAge) {
    return new StorageCleanupProperties(
        StorageCleanupMode.OFF, "0 0 4 * * *", "Asia/Seoul", minimumAge, true);
  }

  private static void assertError(Runnable action, ErrorSpec expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorSpec())
        .isEqualTo(expected);
  }
}
