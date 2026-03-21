package com.sns.marigold.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.dto.ErrorDetail;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 외부에서 생성자 직접 호출 방지
public class ApiResponse<T> {
  private final boolean success;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private final LocalDateTime timestamp;

  private final int status;
  private final String message;

  // 데이터가 null일 경우 JSON 응답에서 필드 자체를 제외하고 싶다면 아래 어노테이션 추가
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final T data;

  private final String errorCode;

  private final List<? extends ErrorDetail> errors;

  // --- 성공 응답 팩토리 메서드 ---
  public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
    return new ApiResponse<>(true, LocalDateTime.now(), status.value(), message, data, null, null);
  }

  // 데이터가 없는 성공 응답 (예: 삭제 성공)
  public static <T> ApiResponse<T> success(HttpStatus status, String message) {
    return new ApiResponse<>(true, LocalDateTime.now(), status.value(), message, null, null, null);
  }

  // --- 에러 응답 팩토리 메서드 ---
  public static <T> ApiResponse<T> error(HttpStatus status, String message) {
    return new ApiResponse<>(false, LocalDateTime.now(), status.value(), message, null, null, null);
  }

  // -- 필드 에러 응답 --
  public static <T> ApiResponse<T> error(
      HttpStatus status, String message, List<? extends ErrorDetail> errors) {
    return new ApiResponse<>(
        false, LocalDateTime.now(), status.value(), message, null, null, errors);
  }

  public static <T> ApiResponse<T> error(ErrorCode errorCode) {
    return new ApiResponse<>(
        false,
        LocalDateTime.now(),
        errorCode.getStatus().value(),
        errorCode.getMessage(),
        null,
        errorCode.getCode(),
        null);
  }

  public static <T> ApiResponse<T> error(ErrorCode errorCode, List<? extends ErrorDetail> errors) {
    return new ApiResponse<>(
        false,
        LocalDateTime.now(),
        errorCode.getStatus().value(),
        errorCode.getMessage(),
        null,
        errorCode.getCode(),
        errors);
  }
}
