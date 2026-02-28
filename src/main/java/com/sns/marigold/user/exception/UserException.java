package com.sns.marigold.user.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class UserException extends BusinessException    {
    protected UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static UserException forUserNotFound() {
        return new UserException(ErrorCode.USER_NOT_FOUND);
    }
    
    public static UserException forUserAlreadyExists() {
        return new UserException(ErrorCode.USER_ALREADY_EXISTS);
    }

    public static UserException forUserNicknameAlreadyExists() {
        return new UserException(ErrorCode.USER_NICKNAME_ALREADY_EXISTS);
    }
}
