package com.sns.marigold.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

  final String REDIRECT_URL;

  public OAuth2FailureHandler(@Value("${app.url.frontend.base}") String baseUrl,
    @Value("${app.url.frontend.login}") String loginUrl) {
    REDIRECT_URL = String.format("%s%s", baseUrl, loginUrl);
  }

  @Override
  public void onAuthenticationFailure(
    HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
    throws IOException, ServletException {
//    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//    response.setContentType("application/json; charset=UTF-8");
//
//    String errorMessage = exception.getMessage();
//    String errorCode = null;
//    String errorDescription = null;
//
//    if (exception.getCause() instanceof org.springframework.security.oauth2.core.OAuth2AuthorizationException) {
//      org.springframework.security.oauth2.core.OAuth2AuthorizationException oae =
//        (org.springframework.security.oauth2.core.OAuth2AuthorizationException) exception.getCause();
//      errorCode = oae.getError().getErrorCode();
//      errorDescription = oae.getError().getDescription();
//    }
//
//    String jsonResponse = String.format(
//      "{ \"isAuthenticated\": \"false\", \"message\": \"%s\", \"errorCode\": \"%s\", \"description\": \"%s\" }",
//      errorMessage.replace("\"", "'"),
//      errorCode,
//      errorDescription
//    );
//
//    response.getWriter().write(jsonResponse);
//    response.getWriter().flush();

    exception.printStackTrace(); // 서버 로그에 전체 스택트레이스

//    String redirectUrl =
//        UriComponentsBuilder.fromUriString(REDIRECT_URL)
//            .queryParam(ERROR_PARAM_PREFIX, exception.getLocalizedMessage())
//            .build()
//            .toUriString();
//
//    getRedirectStrategy().sendRedirect(request, response, REDIRECT_URL);

    response.sendRedirect(REDIRECT_URL);
  }
}
