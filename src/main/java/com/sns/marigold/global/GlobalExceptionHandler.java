package com.sns.marigold.global;

import jakarta.persistence.EntityNotFoundException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  Logger logger = LoggerFactory.getLogger(this.getClass());

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleNotValidException(
      final MethodArgumentNotValidException e) {
    Map<String, String> errors = new HashMap<>();
    e.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    logger.error("MethodArgumentNotValidException : {}", errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleEntityNotFoundException(
      final EntityNotFoundException e) {
    logger.error("EntityNotFoundException : {}", e.getMessage());
    logger.error(e.getMessage(), e);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(SQLException.class)
  public ResponseEntity<Map<String, String>> handleSQLException(final SQLException e) {
    logger.error("SQLException : {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("message", e.getMessage()));
  }
}
