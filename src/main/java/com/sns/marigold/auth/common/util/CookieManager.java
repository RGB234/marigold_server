package com.sns.marigold.auth.common.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
  Refresh Token 쿠키 추가 및 만료 처리
*/
@Component
public class CookieManager {

  public static final String REFRESH_TOKEN_NAME = "refresh_token";
  public static final String RECENT_AUTH_TOKEN_NAME = "recent_auth";

  /**
   * HttpOnly, Secure 쿠키 추가
   *
   * @param response HttpServletResponse
   * @param name 쿠키 이름
   * @param value 쿠키 값
   * @param maxAge 만료 시간(초)
   */
  public void addCookie(HttpServletResponse response, String name, String value, long maxAge) {
    addCookie(response, name, value, maxAge, true);
  }

  public void addReadableCookie(
      HttpServletResponse response, String name, String value, long maxAge) {
    addCookie(response, name, value, maxAge, false);
  }

  private void addCookie(
      HttpServletResponse response, String name, String value, long maxAge, boolean httpOnly) {
    if (response == null || name == null || value == null) {
      return;
    }

    ResponseCookie cookie =
        ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(httpOnly)
            .secure(true) // HTTPS 환경에서만 사용
            .sameSite("None") // cross-site 프론트엔드 요청에도 인증 쿠키를 전송
            .maxAge(maxAge)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  /**
   * 쿠키 만료 처리 (삭제)
   *
   * @param response HttpServletResponse
   * @param name 쿠키 이름
   */
  public void expireCookie(HttpServletResponse response, String name) {
    if (response == null || name == null) {
      return;
    }

    ResponseCookie cookie =
        ResponseCookie.from(name, "")
            .path("/") // 생성했을 때와 동일한 경로
            .httpOnly(true) // 생성 옵션과 동일하게
            .secure(true)
            .sameSite("None")
            .maxAge(0) // 즉시 만료
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  /**
   * 요청에서 특정 이름의 쿠키 객체 가져오기
   *
   * @param request HttpServletRequest
   * @param name 쿠키 이름
   * @return Cookie 객체 (없으면 null)
   */
  public Cookie getCookie(HttpServletRequest request, String name) {
    if (request == null || name == null) {
      return null;
    }

    return WebUtils.getCookie(request, name);
  }
}
