package com.sns.marigold.global.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.dto.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "공통 API 응답 래퍼")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 외부에서 생성자 직접 호출 방지
public class ApiResult<T> {
  @Schema(description = "요청 성공 여부", example = "true")
  private final boolean success;

  @Schema(description = "응답 생성 시각", example = "2026-06-04 12:34:56")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private final LocalDateTime timestamp;

  @Schema(description = "HTTP 상태 코드", example = "200")
  private final int status;

  @Schema(description = "응답 메시지", example = "fetched successfully")
  private final String message;

  // 데이터가 null일 경우 JSON 응답에서 필드 자체를 제외하고 싶다면 아래 어노테이션 추가
  @Schema(description = "응답 데이터")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final T data;

  @Schema(description = "에러 코드. 성공 응답이면 null", example = "INVALID_INPUT_VALUE", nullable = true)
  private final String errorCode;

  @Schema(description = "필드 단위 에러 목록", nullable = true)
  private final List<? extends ErrorDetail> errors;

  // --- 성공 응답 팩토리 메서드 ---
  public static <T> ApiResult<T> success(HttpStatus status, String message, T data) {
    return new ApiResult<>(true, LocalDateTime.now(), status.value(), message, data, null, null);
  }

  // 데이터가 없는 성공 응답 (예: 삭제 성공)
  public static <T> ApiResult<T> success(HttpStatus status, String message) {
    return new ApiResult<>(true, LocalDateTime.now(), status.value(), message, null, null, null);
  }

  // --- 에러 응답 팩토리 메서드 ---
  public static <T> ApiResult<T> error(HttpStatus status, String message) {
    return new ApiResult<>(false, LocalDateTime.now(), status.value(), message, null, null, null);
  }

  // -- 필드 에러 응답 --
  public static <T> ApiResult<T> error(
      HttpStatus status, String message, List<? extends ErrorDetail> errors) {
    return new ApiResult<>(false, LocalDateTime.now(), status.value(), message, null, null, errors);
  }

  public static <T> ApiResult<T> error(ErrorCode errorCode) {
    return new ApiResult<>(
        false,
        LocalDateTime.now(),
        errorCode.getStatus().value(),
        errorCode.getMessage(),
        null,
        errorCode.getCode(),
        null);
  }

  public static <T> ApiResult<T> error(ErrorCode errorCode, List<? extends ErrorDetail> errors) {
    return new ApiResult<>(
        false,
        LocalDateTime.now(),
        errorCode.getStatus().value(),
        errorCode.getMessage(),
        null,
        errorCode.getCode(),
        errors);
  }
}
