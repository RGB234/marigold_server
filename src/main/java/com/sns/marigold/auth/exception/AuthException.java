package com.sns.marigold.auth.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;
import org.springframework.lang.NonNull;

public class AuthException extends BusinessException {

  protected AuthException(@NonNull ErrorCode errorCode) {
    super(errorCode);
  }

  public static AuthException forUnauthorized() {
    return new AuthException(ErrorCode.AUTH_UNAUTHORIZED);
  }

  public static AuthException forAccessDenied() {
    return new AuthException(ErrorCode.AUTH_ACCESS_DENIED);
  }

  public static AuthException forInvalidToken() {
    return new AuthException(ErrorCode.AUTH_TOKEN_INVALID);
  }

  public static AuthException forExpiredToken() {
    return new AuthException(ErrorCode.AUTH_TOKEN_EXPIRED);
  }

  public static AuthException forInternalServerError() {
    return new AuthException(ErrorCode.AUTH_INTERNAL_SERVER_ERROR);
  }

  public static AuthException forInvalidCredentials() {
    return new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS);
  }
}
