package com.sns.marigold.auth.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sns.marigold.global.error.CommonError;
import com.sns.marigold.global.error.FailureReporter;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

import jakarta.servlet.FilterChain;

class SecurityBoundaryExceptionFilterTest {

  @Test
  void unexpectedFilterFailureUsesTheSharedInternalErrorResponse() throws Exception {
    ProblemDetailWriter writer = mock(ProblemDetailWriter.class);
    FailureReporter reporter = mock(FailureReporter.class);
    RuntimeException failure = new RuntimeException("filter failed");
    FilterChain chain =
        (request, response) -> {
          throw failure;
        };
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new SecurityBoundaryExceptionFilter(writer, reporter).doFilter(request, response, chain);

    verify(reporter).reportUnhandled(failure, "security-filter");
    verify(writer).write(request, response, CommonError.INTERNAL_SERVER_ERROR);
  }
}
