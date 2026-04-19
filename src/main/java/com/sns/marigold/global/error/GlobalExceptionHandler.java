package com.sns.marigold.global.error;

import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.global.error.dto.FieldErrorDetail;
import com.sns.marigold.global.error.exception.BusinessException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 전역 예외 처리를 담당하는 Advice 클래스입니다. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 비즈니스 예외 처리. 모든 도메인 커스텀 예외는 BusinessException을 상속합니다. */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<?>> handleBusinessException(
      @NonNull final BusinessException e) {
    log.error("Exception occurred: {}", e.getErrorCode().getCode(), e);
    return ResponseEntity.status(e.getErrorCode().getStatus())
        .body(ApiResponse.error(e.getErrorCode()));
  }

  /** Spring Security의 @PreAuthorize 등에서 발생하는 인가 예외 처리 */
  @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
  public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(
      org.springframework.security.authorization.AuthorizationDeniedException e) {

    // 1. 현재 SecurityContext에서 인증 객체를 가져옵니다.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // 2. 인증 객체가 없거나, 익명 사용자(Anonymous)인 경우 -> 401 Unauthorized
    //
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      log.error("Unauthorized access attempt: {}", e.getMessage());
      return ResponseEntity.status(ErrorCode.AUTH_UNAUTHORIZED.getStatus())
          .body(ApiResponse.error(ErrorCode.AUTH_UNAUTHORIZED));
    }
    // 3. 인증은 되었으나 권한이 부족한 경우 (예: USER가 ADMIN API 호출) -> 403 Forbidden
    log.error("Access denied for user {}: {}", authentication.getName(), e.getMessage());
    return ResponseEntity.status(ErrorCode.AUTH_ACCESS_DENIED.getStatus())
        .body(ApiResponse.error(ErrorCode.AUTH_ACCESS_DENIED));
  }

  /** Request Body 필드 검증 실패 (@Valid) 처리 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(
      final MethodArgumentNotValidException e) {
    log.error("MethodArgumentNotValidException occurred", e);

    BindingResult bindingResult = e.getBindingResult();
    List<FieldErrorDetail> errors =
        bindingResult.getFieldErrors().stream()
            .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());

    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
        .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, errors));
  }

  /** Request Parameter 바인딩/타입 변환 실패 (예: Enum 타입 오류) 처리 */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
      final MethodArgumentTypeMismatchException e) {
    log.error("MethodArgumentTypeMismatchException occurred", e);
    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
        .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE));
  }

  /** 그 외 처리되지 않은 모든 예외 처리 */
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    log.error("Unhandled Exception occurred", e);

    log.error("Exception class: {}", e.getClass());
    log.error("Exception cause: {}", e.getCause());
    log.error("Exception message: {}", e.getMessage());

    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
