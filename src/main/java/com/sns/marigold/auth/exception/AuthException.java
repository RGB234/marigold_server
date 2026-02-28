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

    public static AuthException forAuthorizationDenied() {
        return new AuthException(ErrorCode.AUTH_ACCESS_DENIED);
    }

    public static AuthException forAuthenticationFailed() {
        return new AuthException(ErrorCode.AUTH_TOKEN_INVALID);
    }
}
