package com.sns.marigold.storage.exception;

import com.sns.marigold.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class StorageException extends RuntimeException {

  private static final long serialVersionUID = 1L; // 1. 직렬화 ID 추가

  private final ErrorCode errorCode;

  private StorageException(ErrorCode errorCode, String detailMessage, Throwable cause) {
    super(errorCode.getMessage() + " (" + detailMessage + ")", cause);
    this.errorCode = errorCode;
  }

  // --- Factory Methods ---
  public static StorageException forFileInvalid(String fileName) {
    return new StorageException(ErrorCode.FILE_INVALID, "filename: " + fileName, null);
  }

  public static StorageException forFileInvalid() {
    return new StorageException(ErrorCode.FILE_INVALID, "the file is invalid", null);
  }

  public static StorageException forFileUploadFailed(String fileName, Throwable cause) {
    return new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "filename: " + fileName, cause);
  }

  public static StorageException forFileUploadFailed(Throwable cause) {
    return new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "the file upload failed", cause);
  }
}
