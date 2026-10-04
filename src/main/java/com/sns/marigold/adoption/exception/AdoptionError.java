package com.sns.marigold.adoption.exception;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.FailureKind;

public enum AdoptionError implements ErrorSpec {
  POST_NOT_FOUND("ADOPTION_POST_NOT_FOUND", "존재하지 않는 입양 게시글입니다.", FailureKind.NOT_FOUND),
  POST_ALREADY_COMPLETED(
      "ADOPTION_POST_ALREADY_COMPLETED", "이미 입양 완료된 게시글입니다.", FailureKind.CONFLICT),
  POST_NOT_COMPLETED("ADOPTION_POST_NOT_COMPLETED", "입양 완료 상태가 아닙니다.", FailureKind.CONFLICT),
  POST_DELETED("ADOPTION_POST_DELETED", "삭제된 게시글입니다.", FailureKind.GONE),
  POST_IMAGE_INVALID(
      "ADOPTION_POST_IMAGE_INVALID", "입양 게시글 이미지 정보가 올바르지 않습니다.", FailureKind.INVALID_INPUT),
  COMMENT_NOT_FOUND("ADOPTION_COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다.", FailureKind.NOT_FOUND),
  COMMENT_DELETED("ADOPTION_COMMENT_DELETED", "이미 삭제된 댓글입니다.", FailureKind.CONFLICT),
  COMMENT_POST_MISMATCH(
      "ADOPTION_COMMENT_POST_MISMATCH", "해당 게시글의 댓글이 아닙니다.", FailureKind.INVALID_INPUT),
  COMMENT_IMAGE_INVALID(
      "ADOPTION_COMMENT_IMAGE_INVALID", "댓글 이미지 정보가 올바르지 않습니다.", FailureKind.INVALID_INPUT);

  private final String code;
  private final String publicMessage;
  private final FailureKind kind;

  AdoptionError(String code, String publicMessage, FailureKind kind) {
    this.code = code;
    this.publicMessage = publicMessage;
    this.kind = kind;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String publicMessage() {
    return publicMessage;
  }

  @Override
  public FailureKind kind() {
    return kind;
  }
}
