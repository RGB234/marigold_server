package com.sns.marigold.auth.common.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.util.CookieManager;

import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    List<? extends GrantedAuthority> authorities = new ArrayList<>();
    boolean isAuthenticated = false;

    if (authentication != null) {
      CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();
      isAuthenticated = true;
      authorities = userPrincipal.getAuthorities().stream().toList();
    }

    return new UserAuthStatusDto(isAuthenticated, authorities);
  }
}
