package com.sns.marigold.adoption.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;
import org.springframework.lang.NonNull;

public class AdoptionCommentException extends BusinessException {

  protected AdoptionCommentException(@NonNull ErrorCode errorCode) {
    super(errorCode);
  }

  public static AdoptionCommentException forAdoptionCommentNotFound() {
    return new AdoptionCommentException(ErrorCode.ADOPTION_COMMENT_NOT_FOUND);
  }

  public static AdoptionCommentException forAdoptionCommentDeleted() {
    return new AdoptionCommentException(ErrorCode.ADOPTION_COMMENT_DELETED);
  }

  public static AdoptionCommentException forAdoptionCommentPostMismatch() {
    return new AdoptionCommentException(ErrorCode.ADOPTION_COMMENT_POST_MISMATCH);
  }
}
