package com.sns.marigold.auth.oauth2.handler;

import java.io.IOException;
import java.util.Objects;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sns.marigold.global.config.UrlProperties;
import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.http.RequestIdFilter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 통합 OAuth2 실패 Handler 인증 실패 시 에러 정보를 포함하여 프론트엔드로 리다이렉트합니다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

  private final UrlProperties urlProperties;
  private final AuditLogger auditLogger;
  private final OAuth2FailureMapper failureMapper;
  private final HttpCookieOAuth2AuthorizationRequestRepository
      httpCookieOAuth2AuthorizationRequestRepository;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {

    ErrorSpec publicError = failureMapper.toPublicError(exception);

    // 비정상 callback으로 인해 실패하여 removeAuthorizationRequest() 호출이 되지 않은 경우를 대비한 방어코드
    httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(
        request, response);

    // 콜백 URL로 에러 정보와 함께 리다이렉트
    String callbackUrl = urlProperties.frontend().auth().callback();
    Objects.requireNonNull(callbackUrl, "url.frontend.auth.callback is not configured");

    UriComponentsBuilder redirectBuilder =
        UriComponentsBuilder.fromUriString(callbackUrl)
            .queryParam("error", publicError.code())
            .queryParam("error_description", publicError.publicMessage());
    String requestId = RequestIdFilter.find(request);
    if (requestId != null) {
      redirectBuilder.queryParam("request_id", requestId);
    }
    String redirectUrl = redirectBuilder.build().encode().toUriString();

    auditLogger.warn("event=oauth2_failure errorCode={}", publicError.code());

    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다.");
      return;
    }
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
