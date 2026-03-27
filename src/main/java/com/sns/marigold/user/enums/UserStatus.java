package com.sns.marigold.user.enums;

import com.sns.marigold.auth.common.enums.AuthStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
  ACTIVE("정상"),
  DELETED("탈퇴"),
  BANNED("이용 제한"),
  SLEEP("휴면");

  private final String description;

  public AuthStatus toAuthStatus() {
    return switch (this) {
      case BANNED  -> AuthStatus.BANNED;
      case SLEEP   -> AuthStatus.SLEEP;
      case DELETED -> AuthStatus.DELETED;
      default      -> AuthStatus.LOGIN_SUCCESS;
    };
  }
}
