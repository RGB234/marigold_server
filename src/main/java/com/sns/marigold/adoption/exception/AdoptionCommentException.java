package com.sns.marigold.adoption.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.exception.ApplicationException;

public class AdoptionCommentException extends ApplicationException {

  protected AdoptionCommentException(@NonNull AdoptionError error) {
    super(error);
  }

  public static AdoptionCommentException forAdoptionCommentNotFound() {
    return new AdoptionCommentException(AdoptionError.COMMENT_NOT_FOUND);
  }

  public static AdoptionCommentException forAdoptionCommentDeleted() {
    return new AdoptionCommentException(AdoptionError.COMMENT_DELETED);
  }

  public static AdoptionCommentException forAdoptionCommentPostMismatch() {
    return new AdoptionCommentException(AdoptionError.COMMENT_POST_MISMATCH);
  }

  public static AdoptionCommentException forInvalidCommentImages() {
    return new AdoptionCommentException(AdoptionError.COMMENT_IMAGE_INVALID);
  }
}
