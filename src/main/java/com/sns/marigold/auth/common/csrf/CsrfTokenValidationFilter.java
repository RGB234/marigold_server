package com.sns.marigold.auth.common.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class CsrfTokenValidationFilter extends OncePerRequestFilter {

  private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

  private final CookieManager cookieManager;
  private final ObjectMapper objectMapper;

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
      writeForbidden(response);
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

  private void writeForbidden(HttpServletResponse response) throws IOException {
    response.setStatus(ErrorCode.AUTH_ACCESS_DENIED.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(objectMapper.writeValueAsString(ApiResponse.error(ErrorCode.AUTH_ACCESS_DENIED)));
  }
}
