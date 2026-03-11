package com.sns.marigold.auth.exception;

import com.sns.marigold.global.error.ErrorCode;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException{
    private final ErrorCode errorCode;

    protected AuthException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
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
}
