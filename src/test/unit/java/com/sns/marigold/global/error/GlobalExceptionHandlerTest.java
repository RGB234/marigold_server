package com.sns.marigold.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sns.marigold.audit.AuditLogger;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private AuditLogger auditLogger;
  @Mock private FailureReporter failureReporter;

  @Test
  void handleNoResourceFoundException_ReturnsNotFound() throws Exception {
    GlobalExceptionHandler handler = new GlobalExceptionHandler(auditLogger, failureReporter);
    NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "backup.sql");

    ResponseEntity<Object> response =
        handler.handleException(
            exception, new ServletWebRequest(new MockHttpServletRequest("GET", "/backup.sql")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    ProblemDetail body = (ProblemDetail) response.getBody();
    assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(body.getDetail()).isEqualTo(CommonError.RESOURCE_NOT_FOUND.publicMessage());
    assertThat(body.getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
  }

  @Test
  void springFrameworkClientErrorPreservesItsHttpStatus() throws Exception {
    GlobalExceptionHandler handler = new GlobalExceptionHandler(auditLogger, failureReporter);
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

    ResponseEntity<Object> response =
        handler.handleException(
            exception, new ServletWebRequest(new MockHttpServletRequest("POST", "/read-only")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    ProblemDetail body = (ProblemDetail) response.getBody();
    assertThat(body.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    assertThat(body.getProperties()).containsEntry("errorCode", "INVALID_INPUT_VALUE");
  }
}
