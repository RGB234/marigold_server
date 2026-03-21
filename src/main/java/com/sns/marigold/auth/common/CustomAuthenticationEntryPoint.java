package com.sns.marigold.auth.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.global.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/*
인증 실패 시 처리
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    // Filter(JwtAuthenticationFilter)에서 설정한 상세 에러 코드가 있는지 확인
    ErrorCode errorCode = (ErrorCode) request.getAttribute("exception");

    // 상세 에러가 없으면 기본 '인증 필요(401)' 에러 사용
    if (errorCode == null) {
      errorCode = ErrorCode.AUTH_UNAUTHORIZED;
    }

    ApiResponse<Object> responseBody = ApiResponse.error(errorCode);

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setStatus(errorCode.getStatus().value()); // HTTP 상태 코드 설정

    response.getWriter().write(objectMapper.writeValueAsString(responseBody));
  }
}
