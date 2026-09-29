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
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.chat.ChatDestinations;
import com.sns.marigold.chat.dto.StompErrorResponse;
import com.sns.marigold.global.error.CommonError;
import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.FailureReporter;
import com.sns.marigold.global.error.ValidationViolationMapper;
import com.sns.marigold.global.error.dto.ValidationViolation;
import com.sns.marigold.global.error.exception.ApplicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** {@code @MessageMapping} 처리 중 발생한 복구 가능한 예외를 세션 전용 오류 메시지로 변환합니다. */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class StompExceptionHandler {

  private final AuditLogger auditLogger;
  private final FailureReporter failureReporter;

  @MessageExceptionHandler(ApplicationException.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleApplicationException(
      ApplicationException exception, Message<?> message) {
    failureReporter.report(exception, "stomp-message");
    return response(exception.getErrorSpec(), message, null);
  }

  @MessageExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleAccessDeniedException(Exception exception, Message<?> message) {
    Principal principal = accessor(message).getUser();
    ErrorSpec error =
        principal instanceof Authentication authentication && authentication.isAuthenticated()
            ? AuthError.ACCESS_DENIED
            : AuthError.UNAUTHORIZED;

    if (error == AuthError.ACCESS_DENIED) {
      auditLogger.warn(
          "event=stomp_authorization_denied user={} reason={}",
          principal == null ? null : principal.getName(),
          exception.getMessage());
    } else {
      log.debug("Unauthenticated STOMP message rejected: {}", exception.getMessage());
    }
    return response(error, message, null);
  }

  @MessageExceptionHandler(AuthenticationException.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleAuthenticationException(
      AuthenticationException exception, Message<?> message) {
    log.debug("STOMP authentication not found: {}", exception.getMessage());
    return response(AuthError.UNAUTHORIZED, message, null);
  }

  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleValidationException(
      MethodArgumentNotValidException exception, Message<?> message) {
    List<ValidationViolation> errors = ValidationViolationMapper.from(exception.getBindingResult());
    log.debug("Invalid STOMP message payload. errorCount={}", errors.size());
    return response(CommonError.INVALID_INPUT_VALUE, message, errors);
  }

  @MessageExceptionHandler({
    MessageConversionException.class,
    MethodArgumentResolutionException.class
  })
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleInvalidMessageException(Exception exception, Message<?> message) {
    log.debug("Invalid STOMP message: {}", exception.getMessage());
    return response(CommonError.INVALID_INPUT_VALUE, message, null);
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser(destinations = ChatDestinations.ERROR_QUEUE, broadcast = false)
  public StompErrorResponse handleException(Exception exception, Message<?> message) {
    failureReporter.reportUnhandled(exception, "stomp-message");
    return response(CommonError.INTERNAL_SERVER_ERROR, message, null);
  }

  private StompErrorResponse response(
      ErrorSpec error, Message<?> message, List<ValidationViolation> errors) {
    StompHeaderAccessor accessor = accessor(message);
    StompCommand command = accessor.getCommand();
    return StompErrorResponse.error(
        error, false, command == null ? null : command.name(), accessor.getDestination(), errors);
  }

  private StompHeaderAccessor accessor(Message<?> message) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    return accessor == null ? StompHeaderAccessor.wrap(message) : accessor;
  }
}
