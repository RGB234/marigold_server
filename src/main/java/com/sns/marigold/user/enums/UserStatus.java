package com.sns.marigold.user.enums;

import com.sns.marigold.auth.common.enums.AuthStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "사용자 상태. ACTIVE=정상, DELETED=탈퇴, BANNED=이용 제한, SLEEP=휴면")
@Getter
@AllArgsConstructor
public enum UserStatus {
  @Schema(description = "정상 사용자")
  ACTIVE("정상"),
  @Schema(description = "탈퇴 사용자")
  DELETED("탈퇴"),
  @Schema(description = "이용 제한 사용자")
  BANNED("이용 제한"),
  @Schema(description = "휴면 사용자")
  SLEEP("휴면");

  private final String description;

  public AuthStatus toAuthStatus() {
    return switch (this) {
      case BANNED -> AuthStatus.BANNED;
      case SLEEP -> AuthStatus.SLEEP;
      case DELETED -> AuthStatus.DELETED;
      default -> AuthStatus.LOGIN_SUCCESS;
    };
  }
}
