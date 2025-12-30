package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인 전용 OAuth2 성공 Handler
 * JWT 토큰을 발급하여 프론트엔드로 리다이렉트합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final Environment env;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
  
    log.info("로그인 성공 - UserId: {}, Provider: {}", 
        principal.getUserId(), 
        principal.getOAuth2UserInfo().getProviderInfo());

    // JWT 토큰 생성
    String accessToken = jwtTokenProvider.createAccessToken(principal);
    String refreshToken = jwtTokenProvider.createRefreshToken(principal);

    // 쿠키에 토큰 저장
    Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
    accessTokenCookie.setHttpOnly(true); // XSS 방지
    accessTokenCookie.setSecure(false); // HTTPS 사용 시 true로 변경
    accessTokenCookie.setPath("/");
    accessTokenCookie.setMaxAge(env.getProperty("jwt.access-token-validity-in-seconds", Integer.class, 3600)); // 1H
    
    Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
    refreshTokenCookie.setHttpOnly(true); // XSS 방지
    refreshTokenCookie.setSecure(false); // HTTPS 사용 시 true로 변경
    refreshTokenCookie.setPath("/");
    refreshTokenCookie.setMaxAge(env.getProperty("jwt.refresh-token-validity-in-seconds", Integer.class, 86400)); // 24H

    response.addCookie(accessTokenCookie);
    response.addCookie(refreshTokenCookie);

    // 프론트엔드 홈으로 리다이렉트
    String redirectUrl = env.getProperty("url.frontend.home");
    Objects.requireNonNull(redirectUrl, "url.frontend.home is not configured");

    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다. URL: {}", redirectUrl);
      return;
    }

    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}

