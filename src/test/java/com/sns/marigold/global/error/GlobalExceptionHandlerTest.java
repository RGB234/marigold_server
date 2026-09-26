package com.sns.marigold.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sns.marigold.audit.AuditLogger;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private AuditLogger auditLogger;

  @Test
  void handleNoResourceFoundException_ReturnsNotFound() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler(auditLogger);
    NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "backup.sql");

    ResponseEntity<ProblemDetail> response = handler.handleNoResourceFoundException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(response.getBody().getDetail()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    assertThat(response.getBody().getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
  }
}
