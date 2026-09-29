package com.sns.marigold.auth.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.exception.ApplicationException;

public class AuthException extends ApplicationException {

  protected AuthException(@NonNull AuthError error) {
    super(error);
  }

  protected AuthException(@NonNull AuthError error, Throwable cause) {
    super(error, cause);
  }

  public static AuthException forUnauthorized() {
    return new AuthException(AuthError.UNAUTHORIZED);
  }

  public static AuthException forAccessDenied() {
    return new AuthException(AuthError.ACCESS_DENIED);
  }

  public static AuthException forInvalidToken() {
    return new AuthException(AuthError.TOKEN_INVALID);
  }

  public static AuthException forInvalidToken(Throwable cause) {
    return new AuthException(AuthError.TOKEN_INVALID, cause);
  }

  public static AuthException forExpiredToken() {
    return new AuthException(AuthError.TOKEN_EXPIRED);
  }

  public static AuthException forExpiredToken(Throwable cause) {
    return new AuthException(AuthError.TOKEN_EXPIRED, cause);
  }

  public static AuthException forRecentAuthRequired() {
    return new AuthException(AuthError.RECENT_AUTH_REQUIRED);
  }

  public static AuthException forInternalServerError() {
    return new AuthException(AuthError.INTERNAL_SERVER_ERROR);
  }

  public static AuthException forInternalServerError(Throwable cause) {
    return new AuthException(AuthError.INTERNAL_SERVER_ERROR, cause);
  }

  public static AuthException forInvalidCredentials() {
    return new AuthException(AuthError.INVALID_CREDENTIALS);
  }
}
