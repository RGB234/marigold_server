package com.sns.marigold.auth.exception;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.FailureKind;

public enum AuthError implements ErrorSpec {
  UNAUTHORIZED("AUTH_UNAUTHORIZED", "인증이 필요합니다.", FailureKind.UNAUTHENTICATED),
  ACCESS_DENIED("AUTH_ACCESS_DENIED", "권한이 없습니다.", FailureKind.FORBIDDEN),
  TOKEN_INVALID("AUTH_TOKEN_INVALID", "토큰이 유효하지 않습니다.", FailureKind.UNAUTHENTICATED),
  TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "토큰이 만료되었습니다.", FailureKind.UNAUTHENTICATED),
  RECENT_AUTH_REQUIRED("AUTH_RECENT_AUTH_REQUIRED", "최근 인증이 필요합니다.", FailureKind.FORBIDDEN),
  INVALID_CREDENTIALS(
      "AUTH_INVALID_CREDENTIALS", "이메일이나 비밀번호가 올바르지 않습니다.", FailureKind.INVALID_INPUT),
  INVALID_PROVIDER(
      "AUTH_INVALID_PROVIDER", "지원하지 않는 OAuth2 Provider입니다.", FailureKind.INVALID_INPUT),
  INTERNAL_SERVER_ERROR(
      "AUTH_INTERNAL_SERVER_ERROR", "인증과정에서 서버 오류가 발생했습니다.", FailureKind.INTERNAL),
  OAUTH2_LOGIN_FAILURE(
      "AUTH_OAUTH2_LOGIN_FAILURE", "OAuth2 로그인이 실패했습니다.", FailureKind.INVALID_INPUT),
  OAUTH2_USER_INFO_NOT_FOUND(
      "AUTH_OAUTH2_USER_INFO_NOT_FOUND", "OAuth2 사용자 정보를 찾을 수 없습니다.", FailureKind.INVALID_INPUT);

  private final String code;
  private final String publicMessage;
  private final FailureKind kind;

  AuthError(String code, String publicMessage, FailureKind kind) {
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
