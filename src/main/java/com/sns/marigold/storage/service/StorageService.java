package com.sns.marigold.storage.service;

import java.time.Duration;
import java.util.List;

public interface StorageService {

  /** 삭제 실패를 로그로 남기고 호출자에게 전파하지 않습니다. */
  void deleteFileBestEffort(String storedFileName);

  void deleteFilesBestEffort(List<String> storedFileNames);

  String getViewUrlOrNull(String storedFileName);

  String getDownloadUrl(String storedFileName, String originalFileName);

  /** 클라이언트가 파일을 직접 PUT할 URL을 만듭니다. */
  String createUploadUrl(String storedFileName, String contentType, long size, Duration ttl);

  /** 파일이 없으면 {@code UPLOAD_INCOMPLETE}로 실패합니다. */
  StoredFileHead readFileHead(String storedFileName, int maxBytes);
}
