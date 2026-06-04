package com.sns.marigold.global.error.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 상세 정보")
public interface ErrorDetail {
  @Schema(description = "에러가 발생한 필드명", example = "email")
  String getField();

  @Schema(description = "에러 메시지", example = "이메일 형식이 올바르지 않습니다.")
  String getMessage();
}
