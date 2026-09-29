package com.sns.marigold.auth.common;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/*
 인증 실패 시 처리
*/
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  public static final String AUTH_ERROR_ATTRIBUTE =
      CustomAuthenticationEntryPoint.class.getName() + ".error";

  private final ProblemDetailWriter problemDetailWriter;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    // Filter(JwtAuthenticationFilter)에서 설정한 상세 에러 코드가 있는지 확인
    Object requestError = request.getAttribute(AUTH_ERROR_ATTRIBUTE);
    ErrorSpec error = requestError instanceof ErrorSpec errorSpec ? errorSpec : null;

    // 상세 에러가 없으면 기본 '인증 필요(401)' 에러 사용
    if (error == null) {
      error = AuthError.UNAUTHORIZED;
    }
    problemDetailWriter.write(request, response, error);
  }
}
