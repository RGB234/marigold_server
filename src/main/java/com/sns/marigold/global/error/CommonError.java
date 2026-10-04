package com.sns.marigold.global.error;

public enum CommonError implements ErrorSpec {
  RESOURCE_CONFLICT("RESOURCE_CONFLICT", "이미 존재하는 데이터와 충돌합니다.", FailureKind.CONFLICT),
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.", FailureKind.INTERNAL),
  RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", FailureKind.NOT_FOUND),
  INVALID_INPUT_VALUE("INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다.", FailureKind.INVALID_INPUT);

  private final String code;
  private final String publicMessage;
  private final FailureKind kind;

  CommonError(String code, String publicMessage, FailureKind kind) {
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
