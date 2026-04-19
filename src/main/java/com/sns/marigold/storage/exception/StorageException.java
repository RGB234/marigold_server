package com.sns.marigold.storage.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;
import org.springframework.lang.NonNull;

public class StorageException extends BusinessException {

  private static final long serialVersionUID = 1L;

  private StorageException(
      @NonNull ErrorCode errorCode, @NonNull String detailMessage, Throwable cause) {
    super(errorCode, detailMessage, cause);
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
