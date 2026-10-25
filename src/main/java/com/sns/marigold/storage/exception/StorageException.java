package com.sns.marigold.storage.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.exception.ApplicationException;

public class StorageException extends ApplicationException {

  private StorageException(
      @NonNull StorageError error, @NonNull String diagnosticMessage, Throwable cause) {
    super(error, diagnosticMessage, cause);
  }

  public static StorageException forFileCountExceeded(int count, int max) {
    return new StorageException(
        StorageError.FILE_INVALID, "file count exceeded: count=" + count + ", max=" + max, null);
  }

  public static StorageException forFileSizeExceeded(String fileName, long size, long max) {
    return new StorageException(
        StorageError.FILE_INVALID,
        "file size exceeded: filename=" + fileName + ", size=" + size + ", max=" + max,
        null);
  }

  public static StorageException forMissingFileExtension(String fileName) {
    return new StorageException(
        StorageError.FILE_INVALID, "file has no extension: filename=" + fileName, null);
  }

  public static StorageException forUnsupportedFileExtension(String fileName, String extension) {
    return new StorageException(
        StorageError.FILE_INVALID,
        "unsupported file extension: filename=" + fileName + ", extension=" + extension,
        null);
  }

  public static StorageException forInvalidMimeType(String fileName, String detectedMimeType) {
    return new StorageException(
        StorageError.FILE_INVALID,
        "invalid file mime type: filename=" + fileName + ", detectedMimeType=" + detectedMimeType,
        null);
  }

  public static StorageException forInvalidMimeType(
      String fileName, String extension, String detectedMimeType) {
    return new StorageException(
        StorageError.FILE_INVALID,
        "invalid file mime type: filename="
            + fileName
            + ", extension="
            + extension
            + ", detectedMimeType="
            + detectedMimeType,
        null);
  }

  public static StorageException forFileNotFound() {
    return new StorageException(StorageError.FILE_NOT_FOUND, "the file is not found", null);
  }

  public static StorageException forFileReadFailed(String fileName, Throwable cause) {
    return new StorageException(
        StorageError.FILE_READ_FAILED, "file read failed: filename=" + fileName, cause);
  }

  public static StorageException forFileUploadFailed(Throwable cause) {
    return new StorageException(StorageError.FILE_UPLOAD_FAILED, "the file upload failed", cause);
  }

  public static StorageException forInvalidUploadToken(String reason) {
    return new StorageException(
        StorageError.UPLOAD_TOKEN_INVALID, "invalid upload token: reason=" + reason, null);
  }

  public static StorageException forUploadIncomplete(String storedFileName) {
    return new StorageException(
        StorageError.UPLOAD_INCOMPLETE,
        "uploaded file is missing: storedFileName=" + storedFileName,
        null);
  }

  public static StorageException forUploadSizeMismatch(
      String storedFileName, long expected, long actual) {
    return new StorageException(
        StorageError.UPLOAD_INCOMPLETE,
        "uploaded file size mismatch: storedFileName="
            + storedFileName
            + ", expected="
            + expected
            + ", actual="
            + actual,
        null);
  }
}
