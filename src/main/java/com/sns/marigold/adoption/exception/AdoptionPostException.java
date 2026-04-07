package com.sns.marigold.adoption.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class AdoptionPostException extends BusinessException {
  protected AdoptionPostException(ErrorCode errorCode) {
    super(errorCode);
  }

  public static AdoptionPostException forAdoptionPostNotExists() {
    return new AdoptionPostException(ErrorCode.ADOPTION_POST_NOT_FOUND);
  }

  public static AdoptionPostException forAdoptionPostAlreadyCompleted() {
    return new AdoptionPostException(ErrorCode.ADOPTION_POST_ALREADY_COMPLETED);
  }

  public static AdoptionPostException forAdoptionPostNotCompleted() {
    return new AdoptionPostException(ErrorCode.ADOPTION_POST_NOT_COMPLETED);
  }

  public static AdoptionPostException forAdoptionPostDeleted() {
    return new AdoptionPostException(ErrorCode.ADOPTION_POST_DELETED);
  }
}
