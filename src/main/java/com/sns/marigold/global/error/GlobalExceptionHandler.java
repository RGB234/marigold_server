package com.sns.marigold.global.error;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.global.error.dto.FieldErrorDetail;
import com.sns.marigold.global.error.exception.BusinessException;
import com.sns.marigold.global.error.exception.InternalServerException;
import com.sns.marigold.storage.exception.StorageException;

@ControllerAdvice
public class GlobalExceptionHandler {

  Logger logger = LoggerFactory.getLogger(this.getClass());

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<?>> handleBusinessException(final BusinessException e) {
    logger.error("{} is occurred", e.getErrorCode().getCode());
    logger.error("{}", e.getMessage());
    logger.error("{}", Arrays.stream(e.getStackTrace()).toArray());
    return ResponseEntity.status(e.getErrorCode().getStatus().value()).body(ApiResponse.error(e.getErrorCode()));
  }

  @ExceptionHandler(InternalServerException.class)
  public ResponseEntity<ApiResponse<?>> handleInternalServerException(final InternalServerException e) {
    logger.error("{} is occurred", e.getErrorCode().getCode());
    logger.error("{}", e.getMessage());
    logger.error("{}", Arrays.stream(e.getStackTrace()).toArray());
    return ResponseEntity.status(e.getErrorCode().getStatus().value()).body(ApiResponse.error(e.getErrorCode()));
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiResponse<?>> handleAuthException(final AuthException e) {
    logger.error("{} is occurred", e.getErrorCode().getCode());
    logger.error("{}", e.getMessage());
    logger.error("{}", Arrays.stream(e.getStackTrace()).toArray());
    return ResponseEntity.status(e.getErrorCode().getStatus().value()).body(ApiResponse.error(e.getErrorCode()));
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<ApiResponse<?>> handleStorageException(final StorageException e) {
    logger.error("{} is occurred", e.getErrorCode().getCode());
    logger.error("{}", e.getMessage());
    logger.error("{}", Arrays.stream(e.getStackTrace()).toArray());
    return ResponseEntity.status(e.getErrorCode().getStatus().value()).body(ApiResponse.error(e.getErrorCode()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
    logger.error("{} is occurred", ErrorCode.INVALID_INPUT_VALUE.getCode());
    logger.error("{}", e.getMessage());
    logger.error("{}", Arrays.stream(e.getStackTrace()).toArray());

    // 에러가 발생한 필드들을 리스트로 추출
    List<FieldErrorDetail> errors = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> new FieldErrorDetail(
            error.getField(), // 예: "email"
            error.getDefaultMessage() // 예: "이메일 형식이 올바르지 않습니다."
        ))
        .collect(Collectors.toList());

    // 프론트엔드와 약속한 에러 응답 포맷으로 감싸서 반환
    return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus().value())
        .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, errors));
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
    logger.error("Unhandled Exception", e);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
