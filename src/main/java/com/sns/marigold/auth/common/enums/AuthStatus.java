package com.sns.marigold.auth.common.enums;

public enum AuthStatus {
  LOGIN_SUCCESS, // 기존 유저 정상 로그인
  SIGNUP_SUCCESS, // 신규 가입 직후 로그인
  LINK_SUCCESS, // 기존 유저 소셜 연동 성공
  BANNED, // 제재된 유저
  SLEEP, // 휴면 유저
  DELETED // 탈퇴한 유저
}
