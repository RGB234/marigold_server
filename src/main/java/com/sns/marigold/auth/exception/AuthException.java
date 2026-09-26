package com.sns.marigold.auth.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class AuthException extends BusinessException {

  protected AuthException(@NonNull ErrorCode errorCode) {
    super(errorCode);
  }

  protected AuthException(@NonNull ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
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

  public static AuthException forInvalidToken(Throwable cause) {
    return new AuthException(ErrorCode.AUTH_TOKEN_INVALID, cause);
  }

  public static AuthException forExpiredToken() {
    return new AuthException(ErrorCode.AUTH_TOKEN_EXPIRED);
  }

  public static AuthException forExpiredToken(Throwable cause) {
    return new AuthException(ErrorCode.AUTH_TOKEN_EXPIRED, cause);
  }

  public static AuthException forRecentAuthRequired() {
    return new AuthException(ErrorCode.AUTH_RECENT_AUTH_REQUIRED);
  }

  public static AuthException forInternalServerError() {
    return new AuthException(ErrorCode.AUTH_INTERNAL_SERVER_ERROR);
  }

  public static AuthException forInternalServerError(Throwable cause) {
    return new AuthException(ErrorCode.AUTH_INTERNAL_SERVER_ERROR, cause);
  }

  public static AuthException forInvalidCredentials() {
    return new AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS);
  }
}
