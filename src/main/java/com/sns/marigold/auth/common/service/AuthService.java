package com.sns.marigold.auth.common.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.util.CookieManager;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final CookieManager cookieManager;

  // OAuth2 로그인 -> Spring security 에서 처리 (SecurityConfig & CustomOAuth2UserService

  public void logout(HttpServletResponse response, Authentication authentication) {
    cookieManager.expireCookie(response, "accessToken");
    cookieManager.expireCookie(response, "refreshToken");
    log.info("로그아웃 성공 - 쿠키 삭제 완료");
  }

  public UserAuthStatusDto getAuthStatus(
      Authentication authentication) {
    // 1. Authentication 객체 자체가 없거나, 인증되지 않은 상태(Anonymous)인 경우
    // Spring Security에서 'anonymousUser'는 String 타입이므로 instanceof 체크 필수
    if (authentication == null ||
        !(authentication.getPrincipal() instanceof CustomPrincipal)) {

      return new UserAuthStatusDto(false, Collections.emptyList());
    }

    // 2. 안전하게 캐스팅
    CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();

    // 3. DB 조회 없이 토큰(Principal)에 있는 정보로만 응답 (성능 최적화)
    // JWT 필터를 통과했다면 이미 검증된 사용자라고 신뢰함.
    return new UserAuthStatusDto(
        authentication.isAuthenticated(),
        userPrincipal.getAuthorities().stream().toList());
  }

  // public void reissue(HttpServletRequest request, HttpServletResponse response)
  // {
  // Cookie refreshTokenCookie = cookieManager.getCookie(request, "refreshToken");
  // String refreshToken = refreshTokenCookie.getValue();
  // // 검증
  // Authentication authentication = jwtManager.getAuthentication(refreshToken);
  // CustomPrincipal userPrincipal = (CustomPrincipal)
  // authentication.getPrincipal();

  // String newAccessToken = jwtManager.createAccessToken(userPrincipal);
  // String newRefreshToken = jwtManager.createRefreshToken(userPrincipal);
  // cookieManager.addCookie(response, "accessToken", newAccessToken,
  // jwtManager.getAccessTokenValidityInMilliseconds());
  // cookieManager.addCookie(response, "refreshToken", newRefreshToken,
  // jwtManager.getRefreshTokenValidityInMilliseconds());
  // }
}
