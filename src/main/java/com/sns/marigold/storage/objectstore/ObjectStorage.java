package com.sns.marigold.storage.objectstore;

import java.time.Duration;
import java.util.List;

public interface ObjectStorage {

  /** 삭제 실패를 로그로 남기고 호출자에게 전파하지 않습니다. */
  void deleteFileBestEffort(String storedFileName);

  void deleteFilesBestEffort(List<String> storedFileNames);

  String getViewUrlOrNull(String storedFileName);

  String getDownloadUrl(String storedFileName, String originalFileName);

  /** 클라이언트가 파일을 직접 PUT할 URL을 만듭니다. 파일은 정식 키가 아니라 {@link StorageDirectory#pendingKey}에 올라갑니다. */
  String createUploadUrl(String storedFileName, String contentType, long size, Duration ttl);

  /**
   * 확정 전 업로드({@link StorageDirectory#pendingKey})를 읽습니다. 파일이 없으면 {@code UPLOAD_INCOMPLETE}로 실패합니다.
   */
  StoredFileHead readFileHead(String storedFileName, int maxBytes);

  /**
   * 확정 전 업로드를 정식 키로 옮깁니다. 파일이 없거나 {@code version}이 {@link #readFileHead}로 검사한 뒤 바뀌었으면 {@code
   * UPLOAD_INCOMPLETE}로 실패합니다.
   *
   * @param version {@link StoredFileHead#version()}. null이면 버전을 확인하지 않습니다.
   */
  void promoteUpload(String storedFileName, String version);

  /** 정식 키에 파일이 있으면, 즉 확정을 마친 업로드면 true를 반환합니다. */
  boolean isPromoted(String storedFileName);

  /** 등록되지 않은 업로드의 확정 전 파일과 정식 키 파일을 모두 지웁니다. 실패는 로그로만 남깁니다. */
  void discardUploadBestEffort(String storedFileName);
}
