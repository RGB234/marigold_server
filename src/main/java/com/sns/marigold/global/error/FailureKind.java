package com.sns.marigold.global.error;

/** 전송 방식과 무관한 오류 의미 분류입니다. */
public enum FailureKind {
  INVALID_INPUT,
  UNAUTHENTICATED,
  FORBIDDEN,
  NOT_FOUND,
  CONFLICT,
  GONE,
  LIMIT_EXCEEDED,
  DEPENDENCY_FAILURE,
  // 요청마다 필요한 의존성(인증 세션 저장소 등)이 일시적으로 응답하지 않음. 잠시 뒤 재시도하면 성공할 수 있다
  UNAVAILABLE,
  INTERNAL
}
