package com.sns.marigold.auth.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
@Slf4j
public class CookieManager {

  public final String ACCESS_TOKEN_NAME = "accessToken";
  public final String REFRESH_TOKEN_NAME = "refreshToken";

  /**
   * HttpOnly, Secure 쿠키 추가
   * 
   * @param response HttpServletResponse
   * @param name 쿠키 이름
   * @param value 쿠키 값
   * @param maxAge 만료 시간(초)
   */
  public void addCookie(
      HttpServletResponse response,
      String name,
      String value,
      long maxAge) {
    if (response == null || name == null || value == null) {
      log.warn("쿠키 추가 실패: null 파라미터");
      return;
    }
    
    ResponseCookie cookie = ResponseCookie.from(name, value)
        .path("/")
        .httpOnly(true)
        .secure(true) // 실무 환경은 HTTPS이므로 true 권장
        .sameSite("Lax") // CSRF 방지 및 일반적인 웹 환경에 적합
        .maxAge(maxAge)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    log.debug("쿠키 추가: name={}, maxAge={}초", name, maxAge);
  }

  /**
   * 쿠키 만료 처리 (삭제)
   * 
   * @param response HttpServletResponse
   * @param name 쿠키 이름
   */
  public void expireCookie(
      HttpServletResponse response,
      String name) {
    if (response == null || name == null) {
      log.warn("쿠키 만료 실패: null 파라미터");
      return;
    }
    
    ResponseCookie cookie = ResponseCookie.from(name, "")
        .path("/") // 생성했을 때와 동일한 경로
        .httpOnly(true) // 생성 옵션과 동일하게
        .secure(true)
        .sameSite("Lax")
        .maxAge(0) // 즉시 만료
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    log.debug("쿠키 만료: name={}", name);
  }

  /**
   * 요청에서 특정 이름의 쿠키 객체 가져오기
   * 
   * @param request HttpServletRequest
   * @param name 쿠키 이름
   * @return Cookie 객체 (없으면 null)
   */
  public Cookie getCookie(
      HttpServletRequest request,
      String name) {
    if (request == null || name == null) {
      log.warn("쿠키 조회 실패: null 파라미터");
      return null;
    }
    
    Cookie cookie = WebUtils.getCookie(request, name);
    if (cookie != null) {
      log.debug("쿠키 조회 성공: name={}", name);
    } else {
      log.debug("쿠키 없음: name={}", name);
    }
    
    return cookie;
  }
  
}

