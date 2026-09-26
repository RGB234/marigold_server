package com.sns.marigold.chat.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompConversionException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.chat.dto.StompErrorResponse;
import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 연결을 계속 사용할 수 없는 STOMP 처리 오류를 안전한 JSON {@code ERROR} 프레임으로 변환합니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompProtocolErrorHandler extends StompSubProtocolErrorHandler {

  public static final String ERROR_CODE_HEADER = "error-code";

  private final ObjectMapper objectMapper;

  @Override
  protected Message<byte[]> handleInternal(
      StompHeaderAccessor errorHeaderAccessor,
      byte[] errorPayload,
      @Nullable Throwable cause,
      @Nullable StompHeaderAccessor clientHeaderAccessor) {
    ErrorCode errorCode = resolveErrorCode(cause);
    StompCommand command = clientHeaderAccessor == null ? null : clientHeaderAccessor.getCommand();
    String destination =
        clientHeaderAccessor == null ? null : clientHeaderAccessor.getDestination();
    StompErrorResponse response =
        StompErrorResponse.error(
            errorCode, true, command == null ? null : command.name(), destination);

    if (errorCode.getStatus().is5xxServerError()) {
      log.error("Fatal STOMP processing error", cause);
    } else {
      log.debug(
          "Fatal STOMP message rejected: code={}, command={}, destination={}",
          errorCode.getCode(),
          command,
          destination);
    }

    errorHeaderAccessor.setMessage(errorCode.getMessage());
    errorHeaderAccessor.setNativeHeader(ERROR_CODE_HEADER, errorCode.getCode());
    errorHeaderAccessor.setContentType(MediaType.APPLICATION_JSON);
    return MessageBuilder.createMessage(
        serialize(response), errorHeaderAccessor.getMessageHeaders());
  }

  private ErrorCode resolveErrorCode(@Nullable Throwable cause) {
    BusinessException businessException = findCause(cause, BusinessException.class);
    if (businessException != null) {
      return businessException.getErrorCode();
    }
    if (findCause(cause, StompConversionException.class) != null
        || findCause(cause, MessageConversionException.class) != null) {
      return ErrorCode.BAD_REQUEST;
    }
    if (findCause(cause, AuthenticationException.class) != null) {
      return ErrorCode.AUTH_UNAUTHORIZED;
    }
    if (findCause(cause, AuthorizationDeniedException.class) != null
        || findCause(cause, AccessDeniedException.class) != null) {
      return ErrorCode.AUTH_ACCESS_DENIED;
    }
    return ErrorCode.INTERNAL_SERVER_ERROR;
  }

  @Nullable
  private <T extends Throwable> T findCause(@Nullable Throwable throwable, Class<T> exceptionType) {
    Throwable current = throwable;
    while (current != null) {
      if (exceptionType.isInstance(current)) {
        return exceptionType.cast(current);
      }
      current = current.getCause();
    }
    return null;
  }

  private byte[] serialize(StompErrorResponse response) {
    try {
      return objectMapper.writeValueAsBytes(response);
    } catch (JsonProcessingException exception) {
      log.error("Failed to serialize STOMP error response", exception);
      String fallback =
          "{\"timestamp\":\""
              + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
              + "\",\"errorCode\":\"INTERNAL_SERVER_ERROR\","
              + "\"message\":\"서버 오류가 발생했습니다.\",\"fatal\":true}";
      return fallback.getBytes(StandardCharsets.UTF_8);
    }
  }
}
