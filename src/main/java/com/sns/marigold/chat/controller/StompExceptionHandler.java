package com.sns.marigold.chat.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.chat.ChatDestinations;
import com.sns.marigold.chat.dto.StompErrorResponse;
import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.dto.ErrorDetail;
import com.sns.marigold.global.error.dto.FieldErrorDetail;
import com.sns.marigold.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** {@code @MessageMapping} 처리 중 발생한 복구 가능한 예외를 세션 전용 오류 메시지로 변환합니다. */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class StompExceptionHandler {

  private final AuditLogger auditLogger;

  @MessageExceptionHandler(BusinessException.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleBusinessException(
      BusinessException exception, Message<?> message) {
    ErrorCode errorCode = exception.getErrorCode();
    if (errorCode.getStatus().is5xxServerError()) {
      log.error("STOMP business exception occurred: {}", errorCode.getCode(), exception);
    } else {
      log.debug(
          "STOMP business exception occurred: code={}, message={}",
          errorCode.getCode(),
          exception.getMessage());
    }
    return response(errorCode, message, null);
  }

  @MessageExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleAccessDeniedException(Exception exception, Message<?> message) {
    Principal principal = accessor(message).getUser();
    ErrorCode errorCode =
        principal instanceof Authentication authentication && authentication.isAuthenticated()
            ? ErrorCode.AUTH_ACCESS_DENIED
            : ErrorCode.AUTH_UNAUTHORIZED;

    if (errorCode == ErrorCode.AUTH_ACCESS_DENIED) {
      auditLogger.warn(
          "event=stomp_authorization_denied user={} reason={}",
          principal == null ? null : principal.getName(),
          exception.getMessage());
    } else {
      log.debug("Unauthenticated STOMP message rejected: {}", exception.getMessage());
    }
    return response(errorCode, message, null);
  }

  @MessageExceptionHandler(AuthenticationException.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleAuthenticationException(
      AuthenticationException exception, Message<?> message) {
    log.debug("STOMP authentication not found: {}", exception.getMessage());
    return response(ErrorCode.AUTH_UNAUTHORIZED, message, null);
  }

  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleValidationException(
      MethodArgumentNotValidException exception, Message<?> message) {
    List<FieldErrorDetail> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
    log.debug("Invalid STOMP message payload. errorCount={}", errors.size());
    return response(ErrorCode.INVALID_INPUT_VALUE, message, errors);
  }

  @MessageExceptionHandler({
    MessageConversionException.class,
    MethodArgumentResolutionException.class
  })
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleInvalidMessageException(Exception exception, Message<?> message) {
    log.debug("Invalid STOMP message: {}", exception.getMessage());
    return response(ErrorCode.INVALID_INPUT_VALUE, message, null);
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleException(Exception exception, Message<?> message) {
    log.error("Unhandled STOMP message exception", exception);
    return response(ErrorCode.INTERNAL_SERVER_ERROR, message, null);
  }

  private StompErrorResponse response(
      ErrorCode errorCode, Message<?> message, List<? extends ErrorDetail> errors) {
    StompHeaderAccessor accessor = accessor(message);
    StompCommand command = accessor.getCommand();
    return StompErrorResponse.error(
        errorCode,
        false,
        command == null ? null : command.name(),
        accessor.getDestination(),
        errors);
  }

  private StompHeaderAccessor accessor(Message<?> message) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    return accessor == null ? StompHeaderAccessor.wrap(message) : accessor;
  }
}
