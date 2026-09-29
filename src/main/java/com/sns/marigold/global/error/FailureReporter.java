package com.sns.marigold.global.error;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.sns.marigold.global.error.exception.ApplicationException;

import lombok.extern.slf4j.Slf4j;

/** 경계별 오류 로그 정책을 한 곳에서 관리합니다. */
@Slf4j
@Component
public class FailureReporter {

  public void report(ApplicationException exception, String boundary) {
    report(exception.getErrorSpec(), exception, boundary);
  }

  public void report(ErrorSpec error, @Nullable Throwable cause, String boundary) {
    if (isSystemFailure(error)) {
      if (cause == null) {
        log.error("Application failure: boundary={}, code={}", boundary, error.code());
      } else {
        log.error("Application failure: boundary={}, code={}", boundary, error.code(), cause);
      }
      return;
    }

    log.debug("Application failure: boundary={}, code={}", boundary, error.code());
  }

  public void reportUnhandled(Throwable cause, String boundary) {
    log.error("Unhandled application failure: boundary={}", boundary, cause);
  }

  private boolean isSystemFailure(ErrorSpec error) {
    return error.kind() == FailureKind.INTERNAL || error.kind() == FailureKind.DEPENDENCY_FAILURE;
  }
}
