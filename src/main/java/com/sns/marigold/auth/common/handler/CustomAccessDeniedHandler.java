package com.sns.marigold.auth.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/*
 * 호출 시점: 모든 필터를 거친 후, 마지막 권한 체크 단계(AuthorizationFilter 등)에서 "인증은 되었으나 해당 리소스에 접근할 권한이 없을 때" 호출됩니다.
 * 역할: "이 사용자가 이 행동을 할 자격이 있는가?"를 확인합니다.
 * 동작: 예를 들어, 로그인한 사용자가 ROLE_USER 권한만 있는데, ROLE_ADMIN만 접근 가능한 API를 호출했을 때 AccessDeniedException이 발생하며 이 핸들러가 동작합니다.
 * 특징: JwtAuthenticationFilter에서 인증 객체를 정상적으로 생성했더라도, 그 객체가 가진 권한(Authorities)이 부족할 때 호출되는 시점입니다.
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    log.info(
        "🚫 Access Denied - URI: {}, User: {}, Message: {}",
        request.getRequestURI(),
        request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
        accessDeniedException.getMessage());

    Collection<? extends GrantedAuthority> authorities =
        (request.getUserPrincipal() instanceof Authentication)
            ? ((Authentication) request.getUserPrincipal()).getAuthorities()
            : Collections.emptyList();

    log.info("   - Authorities: {}", authorities);

    ErrorCode errorCode = ErrorCode.AUTH_ACCESS_DENIED;

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setStatus(errorCode.getStatus().value()); // HTTP 상태 코드 설정

    ApiResponse<Object> responseBody = ApiResponse.error(errorCode);
    response.getWriter().write(objectMapper.writeValueAsString(responseBody));
  }
}
