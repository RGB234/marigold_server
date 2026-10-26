package com.sns.marigold.adoption.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.exception.ApplicationException;

public class AdoptionPostException extends ApplicationException {
  protected AdoptionPostException(@NonNull AdoptionError error) {
    super(error);
  }

  public static AdoptionPostException forAdoptionPostNotExists() {
    return new AdoptionPostException(AdoptionError.POST_NOT_FOUND);
  }

  public static AdoptionPostException forAdoptionPostAlreadyCompleted() {
    return new AdoptionPostException(AdoptionError.POST_ALREADY_COMPLETED);
  }

  public static AdoptionPostException forAdoptionPostNotCompleted() {
    return new AdoptionPostException(AdoptionError.POST_NOT_COMPLETED);
  }

  public static AdoptionPostException forAdopterNotCandidate() {
    return new AdoptionPostException(AdoptionError.ADOPTER_NOT_CANDIDATE);
  }

  public static AdoptionPostException forAdoptionCompletionRequired() {
    return new AdoptionPostException(AdoptionError.POST_COMPLETION_REQUIRED);
  }

  public static AdoptionPostException forAdoptionPostDeleted() {
    return new AdoptionPostException(AdoptionError.POST_DELETED);
  }

  public static AdoptionPostException forInvalidPostImages() {
    return new AdoptionPostException(AdoptionError.POST_IMAGE_INVALID);
  }
}
