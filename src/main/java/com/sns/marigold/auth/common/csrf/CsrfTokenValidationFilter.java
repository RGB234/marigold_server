package com.sns.marigold.auth.common.csrf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CsrfTokenValidationFilter extends OncePerRequestFilter {

  private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final String WEBSOCKET_ENDPOINT = "/ws";

  private final CookieManager cookieManager;
  private final ProblemDetailWriter problemDetailWriter;

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    return WEBSOCKET_ENDPOINT.equals(path) || path.startsWith(WEBSOCKET_ENDPOINT + "/");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    if (!UNSAFE_METHODS.contains(request.getMethod()) || !hasCookieAuth(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    Cookie csrfCookie = cookieManager.getCookie(request, CsrfTokenService.CSRF_TOKEN_COOKIE_NAME);
    String csrfHeader = request.getHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME);

    if (csrfCookie == null
        || csrfHeader == null
        || !constantTimeEquals(csrfCookie.getValue(), csrfHeader)) {
      problemDetailWriter.write(request, response, AuthError.ACCESS_DENIED);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean hasCookieAuth(HttpServletRequest request) {
    return cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME) != null
        || cookieManager.getCookie(request, CookieManager.RECENT_AUTH_TOKEN_NAME) != null;
  }

  private boolean constantTimeEquals(String left, String right) {
    if (left == null || right == null) {
      return false;
    }
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }
}
