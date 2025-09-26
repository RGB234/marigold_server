package com.sns.marigold.auth.common.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(
    HttpServletRequest request,
    HttpServletResponse response,
    AccessDeniedException accessDeniedException)
    throws IOException {
    log.error(
      "🚫 Access Denied - URI: {}, User: {}, Message: {}",
      request.getRequestURI(),
      request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
      accessDeniedException.getMessage());

    log.error("{} ", Arrays.stream(accessDeniedException.getStackTrace()).toArray());

    Collection<? extends GrantedAuthority> authorities =
      (request.getUserPrincipal() instanceof Authentication)
        ? ((Authentication) request.getUserPrincipal()).getAuthorities()
        : Collections.emptyList();

    log.error("   - Authorities: {}", authorities);

    response.sendError(HttpServletResponse.SC_FORBIDDEN, "권한이 없습니다.");
  }
}
