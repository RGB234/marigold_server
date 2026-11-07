package com.sns.marigold.storage.objectstore;

import java.util.regex.Pattern;

public enum StorageDirectory {
  USER_PROFILE("user/profile"),
  ADOPTION_POST("adoption/post"),
  ADOPTION_COMMENT("adoption/comment"),
  CHAT_ATTACHMENT("chat/attachment");

  public static final String PENDING_PREFIX = "pending/";

  private static final Pattern STORED_FILE_NAME_PATTERN =
      Pattern.compile("^[0-9a-fA-F-]{36}\\.[A-Za-z0-9]+$");

  private final String path;

  StorageDirectory(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }

  /**
   * 확정 전 업로드가 머무는 키. S3는 이 prefix에 Lifecycle 만료를 건다. {@link #isValidStoredFileName}은 이 키를 거부하므로 고아
   * 정리의 저장소 스캔에서 빠진다.
   */
  public static String pendingKey(String storedFileName) {
    return PENDING_PREFIX + storedFileName;
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
