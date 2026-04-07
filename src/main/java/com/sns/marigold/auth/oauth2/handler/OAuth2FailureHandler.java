package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.global.config.UrlProperties;
import com.sns.marigold.global.error.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** 통합 OAuth2 실패 Handler 인증 실패 시 에러 정보를 포함하여 프론트엔드로 리다이렉트합니다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final UrlProperties urlProperties;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {

    String errorCode = ErrorCode.AUTH_OAUTH2_LOGIN_FAILURE.getCode();
    String errorMessage = exception.getMessage();
    // 콜백 URL로 에러 정보와 함께 리다이렉트

    String callbackUrl = urlProperties.frontend().auth().callback();
    Objects.requireNonNull(callbackUrl, "url.frontend.auth.callback is not configured");

    if (exception instanceof OAuth2AuthenticationException) {
      OAuth2AuthenticationException oAuth2AuthenticationException =
          (OAuth2AuthenticationException) exception;
      OAuth2Error oAuth2Error = oAuth2AuthenticationException.getError();
      errorCode = oAuth2Error.getErrorCode();
      errorMessage = oAuth2Error.getDescription();
    }

    String redirectUrl =
        UriComponentsBuilder.fromUriString(callbackUrl)
            .queryParam("error", URLEncoder.encode(errorCode, StandardCharsets.UTF_8))
            .queryParam(
                "error_description", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
            .build()
            .toUriString();

    log.info("OAuth2 인증 실패: errorCode - {}, errorMessage - {}", errorCode, errorMessage);

    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다. URL: {}", redirectUrl);
      return;
    }
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
