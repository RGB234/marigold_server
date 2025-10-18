package com.sns.marigold.auth.form.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomFailureHandler extends SimpleUrlAuthenticationFailureHandler {
  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException, ServletException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    response.setContentType("application/json; charset=UTF-8");
    // 캐시 방지 헤더 추가
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache"); // HTTP/1.0 표준
    response.setHeader("Expires", "0");

    String jsonResponse = """
        {
              "status": 401,
              "error": "Unauthorized",
              "message": "authentication failed"
        }
        """;
    response.getWriter().write(jsonResponse);
  }
}
