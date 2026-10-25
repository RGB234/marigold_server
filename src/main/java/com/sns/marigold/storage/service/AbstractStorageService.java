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

  protected abstract void deleteStoredFile(String storedFileName) throws IOException;

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
