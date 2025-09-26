package com.sns.marigold.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

//  private final Environment env;

  @Override
  public void onAuthenticationFailure(
    HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
    throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json; charset=UTF-8");
    // 캐시 방지 헤더 추가
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache"); // HTTP/1.0 표준
    response.setHeader("Expires", "0");

    // 응답 JSON 작성
    String errorMessage = exception.getMessage();
    String errorCode = null;
    String errorDescription = null;

    if (exception.getCause() instanceof org.springframework.security.oauth2.core.OAuth2AuthorizationException) {
      org.springframework.security.oauth2.core.OAuth2AuthorizationException oae =
        (org.springframework.security.oauth2.core.OAuth2AuthorizationException) exception.getCause();
      errorCode = oae.getError().getErrorCode();
      errorDescription = oae.getError().getDescription();
    }

    String jsonResponse = String.format(
      "{ \"errorCode\": \"%s\", \"message\": \"%s\", \"description\": \"%s\" }",
      errorMessage.replace("\"", "'"),
      errorCode,
      errorDescription
    );

    response.getWriter().write(jsonResponse);
    response.getWriter().flush();

//    exception.printStackTrace(); // 서버 로그에 전체 스택트레이스

//    response.sendRedirect(env.getProperty("app.url.frontent.login.person"));
  }
}
