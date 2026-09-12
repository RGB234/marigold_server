package com.sns.marigold.storage.service;

import java.util.regex.Pattern;

public enum StorageDirectory {
  USER_PROFILE("user/profile"),
  ADOPTION_POST("adoption/post"),
  ADOPTION_COMMENT("adoption/comment"),
  CHAT_ATTACHMENT("chat/attachment");

  private static final Pattern STORED_FILE_NAME_PATTERN =
      Pattern.compile("^[0-9a-fA-F-]{36}\\.[A-Za-z0-9]+$");

  private final String path;

  StorageDirectory(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }

  public static boolean isValidStoredFileName(String storedFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      return false;
    }

    int fileNameStart = storedFileName.lastIndexOf('/');
    if (fileNameStart < 0 || fileNameStart == storedFileName.length() - 1) {
      return false;
    }

    String directoryPath = storedFileName.substring(0, fileNameStart);
    String fileName = storedFileName.substring(fileNameStart + 1);
    if (!STORED_FILE_NAME_PATTERN.matcher(fileName).matches()) {
      return false;
    }

    for (StorageDirectory directory : values()) {
      if (directory.path.equals(directoryPath)) {
        return true;
      }
    }
    return false;
  }
}
