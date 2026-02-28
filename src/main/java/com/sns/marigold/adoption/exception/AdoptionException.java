package com.sns.marigold.adoption.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class AdoptionException extends BusinessException {
    protected AdoptionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static AdoptionException forAdoptionInfoCompleted() {
        return new AdoptionException(ErrorCode.ADOPTION_INFO_COMPLETED);
    }

    public static AdoptionException forAdoptionInfoNotExists() {
        return new AdoptionException(ErrorCode.ADOPTION_INFO_NOT_FOUND);
    }
}
