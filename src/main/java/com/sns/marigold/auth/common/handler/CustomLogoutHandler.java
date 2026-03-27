package com.sns.marigold.auth.common.handler;

import com.sns.marigold.auth.common.util.CookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** 로그아웃 시 추가적인 로직을 수행하는 핸들러. JWT 토큰 쿠키 만료 처리를 담당합니다. */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

  private final CookieManager cookieManager;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    // log.info("로그아웃 핸들러 실행: 쿠키 만료 처리 시작");

    cookieManager.expireCookie(response, cookieManager.ACCESS_TOKEN_NAME);
    cookieManager.expireCookie(response, cookieManager.REFRESH_TOKEN_NAME);

    // log.info("로그아웃 핸들러 실행 완료: 쿠키 만료 처리 성공");
  }
}
