package com.sns.marigold.global.error;


import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C002", "입력값이 올바르지 않습니다."),

    // Auth
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "A001", "권한이 없습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A002", "토큰이 유효하지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
    
    // User
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U001", "이미 존재하는 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "존재하지 않는 사용자입니다."),
    USER_NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "U003", "이미 존재하는 닉네임입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
