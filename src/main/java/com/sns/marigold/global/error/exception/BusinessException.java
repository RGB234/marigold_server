package com.sns.marigold.global.error.exception;

import com.sns.marigold.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  protected BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  protected BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
  }

  protected BusinessException(ErrorCode errorCode, String detailMessage) {
    super(errorCode.getMessage() + " (" + detailMessage + ")");
    this.errorCode = errorCode;
  }

  protected BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
    super(errorCode.getMessage() + " (" + detailMessage + ")", cause);
    this.errorCode = errorCode;
  }
}
