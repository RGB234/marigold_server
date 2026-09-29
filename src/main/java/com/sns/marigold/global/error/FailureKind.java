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
  INTERNAL
}
