package com.sns.marigold.auth.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 인증 과정에서 발생하는 커스텀 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AuthResponseCode {

  SUCCESS(
    "success",
    "성공"
  ),

  FAILURE(
    "failure",
    "실패"
  ),

  // 로그인 관련 에러
  USER_NOT_REGISTERED(
      "user_not_registered",
      "등록되지 않은 사용자입니다. 회원가입을 먼저 진행해주세요."
  ),
  
  // Provider 관련 에러
  INVALID_PROVIDER(
      "invalid_provider",
      "지원하지 않는 OAuth2 Provider입니다."
  ),
  
  // 사용자 정보 관련 에러
  INVALID_USER_INFO(
      "invalid_user_info",
      "OAuth2 Provider로부터 사용자 정보를 가져올 수 없습니다."
  ),

  USER_ALREADY_REGISTERED(
    "user_already_registered",
    "이미 등록된 사용자입니다."
  );
  
  private final String code;
  private final String description;
}

