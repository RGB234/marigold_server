package com.sns.marigold.auth.common.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Security 인증 실패와 접근 거부를 공통 HTTP 오류 응답으로 변환합니다. */
@Component
@RequiredArgsConstructor
public class SecurityFailureHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  public static final String AUTH_ERROR_ATTRIBUTE =
      SecurityFailureHandler.class.getName() + ".error";

  private final ProblemDetailWriter problemDetailWriter;
  private final AuditLogger auditLogger;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException, ServletException {
    Object requestError = request.getAttribute(AUTH_ERROR_ATTRIBUTE);
    ErrorSpec error =
        requestError instanceof ErrorSpec errorSpec ? errorSpec : AuthError.UNAUTHORIZED;

    problemDetailWriter.write(request, response, error);
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    Collection<? extends GrantedAuthority> authorities =
        request.getUserPrincipal() instanceof Authentication authentication
            ? authentication.getAuthorities()
            : Collections.emptyList();

    auditLogger.warn(
        "event=access_denied path={} user={} authorities={} reason={}",
        request.getRequestURI(),
        request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
        authorities,
        accessDeniedException.getMessage());

    problemDetailWriter.write(request, response, AuthError.ACCESS_DENIED);
  }
}
