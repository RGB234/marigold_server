package com.sns.marigold.user.exception;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.FailureKind;

public enum UserError implements ErrorSpec {
  NOT_FOUND("USER_NOT_FOUND", "존재하지 않는 사용자입니다.", FailureKind.NOT_FOUND),
  ALREADY_EXISTS("USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다.", FailureKind.CONFLICT),
  NICKNAME_ALREADY_EXISTS("USER_NICKNAME_ALREADY_EXISTS", "이미 존재하는 닉네임입니다.", FailureKind.CONFLICT),
  LOCAL_CREDENTIALS_ALREADY_EXISTS(
      "USER_LOCAL_CREDENTIALS_ALREADY_EXISTS",
      "이미 이메일/비밀번호 로그인 정보가 등록된 사용자입니다.",
      FailureKind.CONFLICT),
  OAUTH2_ALREADY_LINKED(
      "USER_OAUTH2_ALREADY_LINKED", "이미 소셜 로그인 정보가 연동된 사용자입니다.", FailureKind.CONFLICT),
  OAUTH2_ACCOUNT_ALREADY_IN_USE(
      "USER_OAUTH2_ACCOUNT_ALREADY_IN_USE", "이미 다른 계정에 연결된 소셜 계정입니다.", FailureKind.CONFLICT),
  DELETED("USER_DELETED", "탈퇴한 사용자입니다.", FailureKind.FORBIDDEN),
  BANNED("USER_BANNED", "이용이 제한된 사용자입니다.", FailureKind.FORBIDDEN),
  SLEEPING("USER_SLEEPING", "휴면 상태인 사용자입니다.", FailureKind.FORBIDDEN);

  private final String code;
  private final String publicMessage;
  private final FailureKind kind;

  UserError(String code, String publicMessage, FailureKind kind) {
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
