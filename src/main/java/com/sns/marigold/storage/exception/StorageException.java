package com.sns.marigold.storage.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class StorageException extends BusinessException {

  private StorageException(
      @NonNull ErrorCode errorCode, @NonNull String detailMessage, Throwable cause) {
    super(errorCode, detailMessage, cause);
  }

  public static StorageException forEmptyFile() {
    return new StorageException(ErrorCode.FILE_INVALID, "file is empty", null);
  }

  public static StorageException forFileCountExceeded(int count, int max) {
    return new StorageException(
        ErrorCode.FILE_INVALID, "file count exceeded: count=" + count + ", max=" + max, null);
  }

  public static StorageException forFileSizeExceeded(String fileName, long size, long max) {
    return new StorageException(
        ErrorCode.FILE_INVALID,
        "file size exceeded: filename=" + fileName + ", size=" + size + ", max=" + max,
        null);
  }

  public static StorageException forTotalFileSizeExceeded(long totalSize, long max) {
    return new StorageException(
        ErrorCode.FILE_INVALID,
        "total file size exceeded: totalSize=" + totalSize + ", max=" + max,
        null);
  }

  public static StorageException forMissingFileExtension(String fileName) {
    return new StorageException(
        ErrorCode.FILE_INVALID, "file has no extension: filename=" + fileName, null);
  }

  public static StorageException forUnsupportedFileExtension(String fileName, String extension) {
    return new StorageException(
        ErrorCode.FILE_INVALID,
        "unsupported file extension: filename=" + fileName + ", extension=" + extension,
        null);
  }

  public static StorageException forInvalidMimeType(String fileName, String detectedMimeType) {
    return new StorageException(
        ErrorCode.FILE_INVALID,
        "invalid file mime type: filename=" + fileName + ", detectedMimeType=" + detectedMimeType,
        null);
  }

  public static StorageException forInvalidMimeType(
      String fileName, String extension, String detectedMimeType) {
    return new StorageException(
        ErrorCode.FILE_INVALID,
        "invalid file mime type: filename="
            + fileName
            + ", extension="
            + extension
            + ", detectedMimeType="
            + detectedMimeType,
        null);
  }

  public static StorageException forFileNotFound() {
    return new StorageException(ErrorCode.FILE_NOT_FOUND, "the file is not found", null);
  }

  public static StorageException forFileReadFailed(String fileName, Throwable cause) {
    return new StorageException(
        ErrorCode.FILE_READ_FAILED, "file read failed: filename=" + fileName, cause);
  }

  public static StorageException forFileUploadFailed(Throwable cause) {
    return new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "the file upload failed", cause);
  }
}
