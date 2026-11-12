package com.sns.marigold.auth.common.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.error.exception.ApplicationException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 로그아웃 시 인증 세션을 폐기하고 refresh 쿠키를 만료시킵니다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomLogoutHandler implements LogoutHandler {

  private final CookieManager cookieManager;
  private final AuthSessionService authSessionService;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    Cookie refreshCookie = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);
    cookieManager.expireCookie(response, CookieManager.REFRESH_TOKEN_NAME);
    if (refreshCookie == null) {
      return;
    }

    // Redis가 응답하지 않아도 브라우저 쿠키는 지워 로그아웃을 마친다. 남은 세션은 만료 시각에 사라진다
    try {
      authSessionService.end(refreshCookie.getValue());
    } catch (ApplicationException e) {
      log.warn("Failed to revoke auth session on logout: code={}", e.getErrorSpec().code());
    }
  }
}
