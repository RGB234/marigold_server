package com.sns.marigold.adoption.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class AdoptionPostException extends BusinessException {
  protected AdoptionPostException(@NonNull ErrorCode errorCode) {
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

  public static AdoptionPostException forInvalidPostImages() {
    return new AdoptionPostException(ErrorCode.ADOPTION_POST_IMAGE_INVALID);
  }
}
