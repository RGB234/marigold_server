package com.sns.marigold.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.dto.ErrorDetail;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** STOMP 메시지 처리 실패를 클라이언트에 전달하는 전용 payload입니다. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StompErrorResponse {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private final LocalDateTime timestamp;

  private final String errorCode;
  private final String message;
  private final boolean fatal;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String command;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String destination;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final List<? extends ErrorDetail> errors;

  public static StompErrorResponse error(
      ErrorCode errorCode, boolean fatal, @Nullable String command, @Nullable String destination) {
    return error(errorCode, fatal, command, destination, null);
  }

  public static StompErrorResponse error(
      ErrorCode errorCode,
      boolean fatal,
      @Nullable String command,
      @Nullable String destination,
      @Nullable List<? extends ErrorDetail> errors) {
    return new StompErrorResponse(
        LocalDateTime.now(),
        errorCode.getCode(),
        errorCode.getMessage(),
        fatal,
        command,
        destination,
        errors);
  }
}
