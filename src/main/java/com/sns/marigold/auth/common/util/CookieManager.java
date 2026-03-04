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
        .secure(true) // HTTPS 환경에서만 사용
        .sameSite("Lax") // 같은 도메인 + 외부 링크에서 접속하는 경우에만 사용 가능
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

