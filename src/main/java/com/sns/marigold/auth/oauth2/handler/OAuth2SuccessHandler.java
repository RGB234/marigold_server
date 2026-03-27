package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.config.UrlProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtManager jwtManager;
  private final CookieManager cookieManager;
  private final UrlProperties urlProperties;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
    AuthStatus authStatus = principal.getAuthStatus();
    log.info("OAuth2 인증 성공 - UserId: {}, 상태: {}", principal.getUserId(), authStatus);

    // 정상 상태(로그인, 신규가입)일 때만 토큰 및 쿠키 발급
    if (authStatus == AuthStatus.LOGIN_SUCCESS || authStatus == AuthStatus.SIGNUP_SUCCESS) {
      // 1. JWT 토큰 생성
      String accessToken = jwtManager.createAccessToken(principal);
      String refreshToken = jwtManager.createRefreshToken(principal);

      // 2. 쿠키 설정 및 추가
      cookieManager.addCookie(
          response,
          cookieManager.ACCESS_TOKEN_NAME,
          accessToken,
          jwtManager.getAccessTokenValidityInMilliseconds());
      cookieManager.addCookie(
          response,
          cookieManager.REFRESH_TOKEN_NAME,
          refreshToken,
          jwtManager.getRefreshTokenValidityInMilliseconds());
    }

    String callbackUrl = urlProperties.frontend().auth().callback();
    Objects.requireNonNull(callbackUrl, "url.frontend.auth.callback is not configured");

    String redirectUrl =
        UriComponentsBuilder.fromUriString(callbackUrl)
            .queryParam("auth_status", authStatus.name())
            .build()
            .toUriString();

    // 3. 리다이렉트 처리
    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다.");
      return;
    }
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}