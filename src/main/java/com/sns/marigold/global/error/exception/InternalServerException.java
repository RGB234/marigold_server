package com.sns.marigold.global.error.exception;

import com.sns.marigold.global.error.ErrorCode;
import org.springframework.lang.NonNull;

public class InternalServerException extends BusinessException {

  protected InternalServerException(@NonNull ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }

  public static InternalServerException forInternalServerError(Throwable cause) {
    return new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
  }
}
