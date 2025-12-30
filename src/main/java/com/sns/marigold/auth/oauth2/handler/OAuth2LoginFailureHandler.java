package com.sns.marigold.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.sns.marigold.auth.common.enums.AuthResponseCode;

/**
 * 로그인 전용 OAuth2 실패 Handler
 * 로그인 실패 시 에러 정보를 포함하여 프론트엔드로 리다이렉트합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final Environment env;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception)
      throws IOException, ServletException {

    String errorCode = AuthResponseCode.FAILURE.getCode();
    String errorMessage = exception.getMessage();
    // 로그인 콜백 URL로 에러 정보와 함께 리다이렉트
    String redirectUrl = env.getProperty("url.frontend.auth.login");
    Objects.requireNonNull(redirectUrl, "url.frontend.auth.login is not configured");

    if (exception instanceof OAuth2AuthenticationException) {
      OAuth2AuthenticationException oAuth2AuthenticationException = (OAuth2AuthenticationException) exception;
      OAuth2Error oAuth2Error = oAuth2AuthenticationException.getError();
      errorCode = oAuth2Error.getErrorCode();
      errorMessage = oAuth2Error.getDescription();
    }

    String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
    .queryParam("code", errorCode)
    .build().toUriString();


    log.info("로그인 실패: errorCode - {}, errorMessage - {}", errorCode, errorMessage);

    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다. URL: {}", targetUrl);
      return;
    }
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}

