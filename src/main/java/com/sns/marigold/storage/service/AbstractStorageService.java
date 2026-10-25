package com.sns.marigold.storage.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.sns.marigold.storage.exception.StorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStorageService implements StorageService {

  @Override
  public void deleteFileBestEffort(String storedFileName) {
    Objects.requireNonNull(storedFileName, "storedFileName must not be null");
    try {
      validateStoredFileName(storedFileName);
      deleteStoredFile(storedFileName);
    } catch (Exception e) {
      log.warn("event=storage_delete_failed storedFileName={}", storedFileName, e);
    }
  }

  @Override
  public void deleteFilesBestEffort(List<String> storedFileNames) {
    for (String storedFileName : storedFileNames) {
      this.deleteFileBestEffort(storedFileName);
    }
  }

  @Override
  public void discardUploadBestEffort(String storedFileName) {
    Objects.requireNonNull(storedFileName, "storedFileName must not be null");
    try {
      validateStoredFileName(storedFileName);
      deleteStoredFile(StorageDirectory.pendingKey(storedFileName));
      // 확정은 했지만 도메인 저장이 실패한 경우 정식 키에도 남아 있다
      deleteStoredFile(storedFileName);
    } catch (Exception e) {
      log.warn("event=storage_discard_failed storedFileName={}", storedFileName, e);
    }
  }

  /** 키를 검증하지 않고 지웁니다. 호출자가 {@link #validateStoredFileName}으로 검증한 키만 넘깁니다. */
  protected abstract void deleteStoredFile(String key) throws IOException;

  protected String sanitizeContentDispositionFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "download";
    }
    return fileName.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
  }

  protected void validateStoredFileName(String storedFileName) {
    if (!StorageDirectory.isValidStoredFileName(storedFileName)) {
      throw StorageException.forFileNotFound();
    }
  }
}
