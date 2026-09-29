package com.sns.marigold.global.error.dto;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입력값 검증 오류")
public record ValidationViolation(
    @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "입력 위치", example = "body")
        @Nullable
        String location,
    @Schema(description = "오류가 발생한 필드명", example = "email") String field,
    @Schema(description = "오류 메시지", example = "이메일 형식이 올바르지 않습니다.") String message) {

  public static ValidationViolation body(String field, String message) {
    return new ValidationViolation("body", field, message);
  }

  public static ValidationViolation parameter(String field, String message) {
    return new ValidationViolation("parameter", field, message);
  }
}
