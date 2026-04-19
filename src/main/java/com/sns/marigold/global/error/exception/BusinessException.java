package com.sns.marigold.global.error.exception;

import com.sns.marigold.global.error.ErrorCode;
import lombok.Getter;
import org.springframework.lang.NonNull;

@Getter
public class BusinessException extends RuntimeException {
  @NonNull private final ErrorCode errorCode;

  protected BusinessException(@NonNull ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  protected BusinessException(@NonNull ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  protected BusinessException(@NonNull ErrorCode errorCode, @NonNull String detailMessage) {
    super(errorCode.getMessage() + " (" + detailMessage + ")");
    this.errorCode = errorCode;
  }

  protected BusinessException(
      @NonNull ErrorCode errorCode, @NonNull String detailMessage, Throwable cause) {
    super(errorCode.getMessage() + " (" + detailMessage + ")", cause);
    this.errorCode = errorCode;
  }
}
