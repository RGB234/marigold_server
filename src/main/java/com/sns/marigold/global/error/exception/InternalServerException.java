package com.sns.marigold.global.error.exception;

import com.sns.marigold.global.error.ErrorCode;

import lombok.Getter;

@Getter
public class InternalServerException extends RuntimeException{
    private final ErrorCode errorCode;

    protected InternalServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public static InternalServerException forInternalServerError(Throwable cause) {
        return new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
