package com.sns.marigold.auth.common.jwt;

import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.error.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

/*
 * 동작: 요청에 포함된 JWT 토큰을 꺼내서 유효한지 검사하고, 유효하다면 SecurityContextHolder에 인증 객체(Authentication)를 저장.
 * 특징: 여기서 토큰이 없거나 만료되어도 필터 체인을 중단하지 않고 예외를 기록한 다음 다음 필터로 넘기고, CustomAuthenticationEntryPoint에서 예외처리.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtManager jwtManager;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // 1. Request에서 토큰 추출
    String accessToken = resolveToken(request, CookieManager.ACCESS_TOKEN_NAME);

    // 2. 토큰이 존재하는 경우 검증 및 인증 처리
    if (StringUtils.hasText(accessToken)) {
      try {
        // 유효한 토큰인 경우 인증 객체 생성. 아닐 경우 예외 발생
        Authentication authentication = jwtManager.getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

      } catch (ExpiredJwtException e) {
        log.info("Access Token 만료: {}", e.getMessage());
        request.setAttribute("exception", ErrorCode.AUTH_TOKEN_EXPIRED);
      } catch (JwtException | IllegalArgumentException e) {
        log.warn("유효하지 않은 토큰: {}", e.getMessage());
        request.setAttribute("exception", ErrorCode.AUTH_TOKEN_INVALID);
      }
    }

    // 3. 다음 필터로 진행
    filterChain.doFilter(request, response);
  }

  /** 요청 쿠키에서 JWT 토큰 추출 */
  @Nullable
  private String resolveToken(HttpServletRequest request, String cookieName) {
    Cookie cookie = WebUtils.getCookie(request, cookieName);
    return (cookie != null) ? cookie.getValue() : null;
  }
}
