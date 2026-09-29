package com.sns.marigold.global.error.exception;

import java.util.Objects;

import com.sns.marigold.global.error.ErrorSpec;

/** 호출자가 예측하고 처리할 수 있는 애플리케이션 오류입니다. */
public abstract class ApplicationException extends RuntimeException {

  private final ErrorSpec errorSpec;

  protected ApplicationException(ErrorSpec errorSpec) {
    super(Objects.requireNonNull(errorSpec).publicMessage());
    this.errorSpec = errorSpec;
  }

  protected ApplicationException(ErrorSpec errorSpec, Throwable cause) {
    super(Objects.requireNonNull(errorSpec).publicMessage(), cause);
    this.errorSpec = errorSpec;
  }

  protected ApplicationException(ErrorSpec errorSpec, String diagnosticMessage, Throwable cause) {
    super(Objects.requireNonNull(diagnosticMessage), cause);
    this.errorSpec = Objects.requireNonNull(errorSpec);
  }

  public final ErrorSpec getErrorSpec() {
    return errorSpec;
  }
}
