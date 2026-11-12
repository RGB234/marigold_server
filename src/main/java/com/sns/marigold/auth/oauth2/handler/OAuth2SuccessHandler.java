package com.sns.marigold.auth.oauth2.handler;

import java.io.IOException;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.common.session.SessionTokens;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.config.UrlProperties;
import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.exception.ApplicationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AuthSessionService authSessionService;
  private final CookieManager cookieManager;
  private final UrlProperties urlProperties;
  private final AuditLogger auditLogger;
  private final OAuth2FailureHandler failureHandler;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
    AuthStatus authStatus = principal.getAuthStatus();
    auditLogger.info(
        "event=oauth2_success userId={} authStatus={}", principal.getUserId(), authStatus);

    // 정상 상태(로그인, 신규가입)일 때만 토큰 및 쿠키 발급
    if (authStatus == AuthStatus.LOGIN_SUCCESS || authStatus == AuthStatus.SIGNUP_SUCCESS) {
      // 1. 세션을 Redis에 저장. 실패하면 쿠키 없이 오류 redirect로 보낸다
      SessionTokens tokens;
      try {
        tokens = authSessionService.start(principal);
      } catch (ApplicationException e) {
        ErrorSpec error = e.getErrorSpec();
        failureHandler.onAuthenticationFailure(
            request,
            response,
            new OAuth2AuthenticationException(
                new OAuth2Error(error.code(), error.publicMessage(), null), e));
        return;
      }

      // 2. 새 쿠키가 이전 쿠키를 덮어쓰면 이전 세션은 아무도 쓰지 않은 채 만료까지 남으므로 지금 폐기한다
      Cookie previous = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);
      if (previous != null) {
        authSessionService.endPrevious(previous.getValue());
      }

      // 3. Refresh 토큰
      cookieManager.addCookie(
          response,
          CookieManager.REFRESH_TOKEN_NAME,
          tokens.refreshToken(),
          tokens.refreshMaxAgeSeconds());
    }

    String callbackUrl = urlProperties.frontend().auth().callback();
    Objects.requireNonNull(callbackUrl, "url.frontend.auth.callback is not configured");

    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUriString(callbackUrl)
            .queryParam("auth_status", authStatus.name());

    String redirectUrl = uriBuilder.build().toUriString();

    // 4. 리다이렉션 처리
    if (response.isCommitted()) {
      log.warn("응답이 이미 커밋되어 리다이렉트 할 수 없습니다.");
      return;
    }
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
