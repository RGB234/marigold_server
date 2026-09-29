package com.sns.marigold.global.error.http;

import org.springframework.http.HttpStatus;

import com.sns.marigold.global.error.ErrorSpec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 애플리케이션 오류 의미를 HTTP 상태로 변환합니다. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpErrorPolicy {

  public static HttpStatus statusOf(ErrorSpec error) {
    return switch (error.kind()) {
      case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
      case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case GONE -> HttpStatus.GONE;
      case LIMIT_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
      case DEPENDENCY_FAILURE, INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
