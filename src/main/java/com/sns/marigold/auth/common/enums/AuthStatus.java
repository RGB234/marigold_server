package com.sns.marigold.auth.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 처리 결과 상태")
public enum AuthStatus {
  @Schema(description = "기존 사용자 정상 로그인")
  LOGIN_SUCCESS, // 기존 유저 정상 로그인
  @Schema(description = "신규 가입 직후 로그인")
  SIGNUP_SUCCESS, // 신규 가입 직후 로그인
  @Schema(description = "기존 사용자 소셜 연동 성공")
  LINK_SUCCESS, // 기존 유저 소셜 연동 성공
  @Schema(description = "제재된 사용자")
  BANNED, // 제재된 유저
  @Schema(description = "휴면 사용자")
  SLEEP, // 휴면 유저
  @Schema(description = "탈퇴한 사용자")
  DELETED // 탈퇴한 유저
}
