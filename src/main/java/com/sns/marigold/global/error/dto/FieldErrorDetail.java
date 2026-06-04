package com.sns.marigold.global.error.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "필드 검증 에러 상세 정보")
@Getter
public class FieldErrorDetail implements ErrorDetail {
  @Schema(description = "에러가 발생한 필드명", example = "email")
  private final String field;

  @Schema(description = "에러 메시지", example = "이메일 형식이 올바르지 않습니다.")
  private final String message;

  public FieldErrorDetail(String field, String message) {
    this.field = field;
    this.message = message;
  }
}
