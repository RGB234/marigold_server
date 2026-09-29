package com.sns.marigold.storage.exception;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.FailureKind;

public enum StorageError implements ErrorSpec {
  FILE_INVALID("FILE_INVALID", "파일이 올바르지 않습니다.", FailureKind.INVALID_INPUT),
  FILE_TOO_LARGE("FILE_TOO_LARGE", "파일 또는 요청의 최대 업로드 용량을 초과했습니다.", FailureKind.LIMIT_EXCEEDED),
  FILE_NOT_FOUND("FILE_NOT_FOUND", "파일을 찾을 수 없습니다.", FailureKind.NOT_FOUND),
  FILE_READ_FAILED("FILE_READ_FAILED", "파일을 읽는 중 오류가 발생했습니다.", FailureKind.DEPENDENCY_FAILURE),
  FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다.", FailureKind.DEPENDENCY_FAILURE);

  private final String code;
  private final String publicMessage;
  private final FailureKind kind;

  StorageError(String code, String publicMessage, FailureKind kind) {
    this.code = code;
    this.publicMessage = publicMessage;
    this.kind = kind;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String publicMessage() {
    return publicMessage;
  }

  @Override
  public FailureKind kind() {
    return kind;
  }
}
