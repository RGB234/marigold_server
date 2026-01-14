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
    // JwtAuthenticationFilter에서 Authentication 객체 생성 후 SecurityContext에 저장함
    // Spring Security에서 'anonymousUser'는 String 타입이므로 instanceof 체크 필수
    if (authentication == null ||
        !(authentication.getPrincipal() instanceof CustomPrincipal)) {

      return new UserAuthStatusDto(null, Collections.emptyList());
    }

    // 2. 안전하게 캐스팅
    CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();

    // 3. DB 조회 없이 토큰(Principal)에 있는 정보로만 응답 (성능 최적화)
    // JWT 필터를 통과했다면 이미 검증된 사용자라고 신뢰함.
    // 보안상 민감한 부분에서는 DB에서 사용자 정보를 조회하여 검증하도록 함.
    return new UserAuthStatusDto(
        userPrincipal.getUserId(),
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
