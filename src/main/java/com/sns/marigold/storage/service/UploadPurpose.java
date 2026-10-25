package com.sns.marigold.storage.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sns.marigold.global.validation.ValidationPolicy;

/** 업로드 발급 요청의 용도. 저장 위치와 개수, 크기, 허용 형식을 함께 정한다. */
public enum UploadPurpose {
  ADOPTION_POST_IMAGE(
      StorageDirectory.ADOPTION_POST,
      ValidationPolicy.AdoptionPost.IMAGE_COUNT.max(),
      ValidationPolicy.Image.MAX_SIZE_BYTES,
      ValidationPolicy.Image.ALLOWED_MIME_TYPES_BY_EXTENSION),
  ADOPTION_COMMENT_IMAGE(
      StorageDirectory.ADOPTION_COMMENT,
      ValidationPolicy.Comment.IMAGE_COUNT.max(),
      ValidationPolicy.Image.MAX_SIZE_BYTES,
      ValidationPolicy.Image.ALLOWED_MIME_TYPES_BY_EXTENSION),
  USER_PROFILE_IMAGE(
      StorageDirectory.USER_PROFILE,
      1,
      ValidationPolicy.Image.MAX_SIZE_BYTES,
      ValidationPolicy.Image.ALLOWED_MIME_TYPES_BY_EXTENSION),
  CHAT_ATTACHMENT(
      StorageDirectory.CHAT_ATTACHMENT,
      ValidationPolicy.ChatAttachment.MAX_COUNT,
      ValidationPolicy.ChatAttachment.MAX_FILE_SIZE_BYTES,
      ValidationPolicy.ChatAttachment.ALLOWED_MIME_TYPES_BY_EXTENSION);

  private final StorageDirectory directory;
  private final int maxCount;
  private final long maxSizeBytes;
  private final Map<String, List<String>> allowedMimeTypesByExtension;

  UploadPurpose(
      StorageDirectory directory,
      int maxCount,
      long maxSizeBytes,
      Map<String, List<String>> allowedMimeTypesByExtension) {
    this.directory = directory;
    this.maxCount = maxCount;
    this.maxSizeBytes = maxSizeBytes;
    this.allowedMimeTypesByExtension = allowedMimeTypesByExtension;
  }

  public StorageDirectory directory() {
    return directory;
  }

  public int maxCount() {
    return maxCount;
  }

  public long maxSizeBytes() {
    return maxSizeBytes;
  }

  /** 확장자(소문자, 점 제외)에 허용된 감지 MIME 목록. 허용되지 않은 확장자면 null. */
  public List<String> allowedMimeTypes(String extension) {
    return allowedMimeTypesByExtension.get(extension);
  }

  public static Optional<UploadPurpose> ofStoredFileName(String storedFileName) {
    for (UploadPurpose purpose : values()) {
      if (storedFileName.startsWith(purpose.directory.path() + "/")) {
        return Optional.of(purpose);
      }
    }
    return Optional.empty();
  }
}
