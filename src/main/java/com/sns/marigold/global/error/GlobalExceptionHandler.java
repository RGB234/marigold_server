package com.sns.marigold.global.error;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sns.marigold.global.error.exception.BusinessException;


@ControllerAdvice
public class GlobalExceptionHandler {

  Logger logger = LoggerFactory.getLogger(this.getClass());

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(final BusinessException e) {
    logger.error("{} is occurred", e.getErrorCode());
    logger.error("{}", e.getMessage());
    logger.error("{}", Arrays.stream(e.getStackTrace()).toArray());
    return ErrorResponse.toResponseEntity(e.getErrorCode());
  }
}
