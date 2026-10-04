package com.sns.marigold.global.error;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.global.error.dto.ValidationViolation;
import com.sns.marigold.global.error.exception.ApplicationException;
import com.sns.marigold.global.error.http.HttpErrorPolicy;
import com.sns.marigold.global.error.http.ProblemDetailFactory;
import com.sns.marigold.storage.exception.StorageError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** MVC 경계에서 애플리케이션 오류를 RFC 9457 Problem Details로 변환합니다. */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private final AuditLogger auditLogger;
  private final FailureReporter failureReporter;

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    ErrorSpec error = UniqueConflictPolicy.resolve(exception);
    if (error == null) {
      return handleUnhandledException(exception, request);
    }
    log.debug("Database unique conflict: errorCode={}", error.code());
    return problem(error, request, null);
  }

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ProblemDetail> handleApplicationException(
      @NonNull ApplicationException exception, HttpServletRequest request) {
    failureReporter.report(exception, "http");
    return problem(exception.getErrorSpec(), request, null);
  }

  @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAuthorizationDeniedException(
      org.springframework.security.authorization.AuthorizationDeniedException exception,
      HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      log.debug("Unauthorized access attempt: {}", exception.getMessage());
      return problem(AuthError.UNAUTHORIZED, request, null);
    }

    auditLogger.warn(
        "event=authorization_denied user={} reason={}",
        authentication.getName(),
        exception.getMessage());
    return problem(AuthError.ACCESS_DENIED, request, null);
  }

  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleAuthenticationCredentialsNotFoundException(
      AuthenticationCredentialsNotFoundException exception, HttpServletRequest request) {
    log.debug("Authentication not found in security context: {}", exception.getMessage());
    return problem(AuthError.UNAUTHORIZED, request, null);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<ValidationViolation> errors =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    ValidationViolation.parameter(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
    return problem(CommonError.INVALID_INPUT_VALUE, request, errors);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnhandledException(
      Exception exception, HttpServletRequest request) {
    failureReporter.reportUnhandled(exception, "http");
    return problem(CommonError.INTERNAL_SERVER_ERROR, request, null);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ValidationViolation> errors = ValidationViolationMapper.from(exception.getBindingResult());
    log.debug("Invalid HTTP request body. errorCount={}", errors.size());
    return frameworkProblem(CommonError.INVALID_INPUT_VALUE, status, headers, request, errors);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ValidationViolation> errors =
        exception.getAllErrors().stream().map(ValidationViolationMapper::from).toList();
    return frameworkProblem(CommonError.INVALID_INPUT_VALUE, status, headers, request, errors);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ErrorSpec error = frameworkError(exception, status);
    if (status.is5xxServerError()) {
      failureReporter.reportUnhandled(exception, "http-framework");
    } else {
      log.debug(
          "HTTP framework exception: type={}, status={}",
          exception.getClass().getSimpleName(),
          status.value());
    }
    return frameworkProblem(error, status, headers, request, null);
  }

  private ErrorSpec frameworkError(Exception exception, HttpStatusCode status) {
    if (exception instanceof NoResourceFoundException) {
      return CommonError.RESOURCE_NOT_FOUND;
    }
    if (exception instanceof MaxUploadSizeExceededException) {
      return StorageError.FILE_TOO_LARGE;
    }
    return status.is5xxServerError()
        ? CommonError.INTERNAL_SERVER_ERROR
        : CommonError.INVALID_INPUT_VALUE;
  }

  private ResponseEntity<ProblemDetail> problem(
      ErrorSpec error, HttpServletRequest request, List<ValidationViolation> validationErrors) {
    HttpStatusCode status = HttpErrorPolicy.statusOf(error);
    ProblemDetail body = ProblemDetailFactory.create(error, status, request, validationErrors);
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
  }

  private ResponseEntity<Object> frameworkProblem(
      ErrorSpec error,
      HttpStatusCode status,
      HttpHeaders sourceHeaders,
      WebRequest webRequest,
      List<ValidationViolation> validationErrors) {
    HttpHeaders headers = new HttpHeaders();
    headers.putAll(sourceHeaders);
    headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    HttpServletRequest request =
        webRequest instanceof ServletWebRequest servletWebRequest
            ? servletWebRequest.getRequest()
            : null;
    ProblemDetail body = ProblemDetailFactory.create(error, status, request, validationErrors);
    return new ResponseEntity<>(body, headers, status);
  }
}
