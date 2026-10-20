package com.sns.marigold.auth.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.handler.SecurityFailureHandler;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

class SecurityFailureHandlerTest {

  private final ProblemDetailWriter problemDetailWriter = mock(ProblemDetailWriter.class);
  private final AuditLogger auditLogger = mock(AuditLogger.class);
  private final SecurityFailureHandler handler =
      new SecurityFailureHandler(problemDetailWriter, auditLogger);

  @Test
  void authenticationFailureUsesUnauthorizedByDefault() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.commence(request, response, mock(AuthenticationException.class));

    verify(problemDetailWriter).write(request, response, AuthError.UNAUTHORIZED);
  }

  @Test
  void authenticationFailureUsesErrorStoredByJwtFilter() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setAttribute(SecurityFailureHandler.AUTH_ERROR_ATTRIBUTE, AuthError.TOKEN_EXPIRED);

    handler.commence(request, response, mock(AuthenticationException.class));

    verify(problemDetailWriter).write(request, response, AuthError.TOKEN_EXPIRED);
  }

  @Test
  void accessDeniedUsesForbiddenError() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(request, response, new AccessDeniedException("denied"));

    verify(problemDetailWriter).write(request, response, AuthError.ACCESS_DENIED);
  }
}
