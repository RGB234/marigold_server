package com.sns.marigold.auth.common.jwt;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtManager jwtManager;
  @Value("${jwt.access-token-validity-in-seconds}")
  private long accessTokenValidityInSeconds;
  @Value("${jwt.refresh-token-validity-in-seconds}")
  private long refreshTokenValidityInSeconds;

  @Override
protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

    // 1. Request에서 토큰 문자열만 추출 (여기서는 검증하지 않음)
    String accessToken = resolveAccessToken(request); 

    // 2. 토큰이 있는 경우 검증 시작
    if (StringUtils.hasText(accessToken)) {
        try {
            // 토큰 검증 및 인증 객체 생성
            Authentication authentication = jwtManager.getAuthentication(accessToken);
            
            // SecurityContext에 저장 (인증 완료)
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            // 토큰 만료 별도 처리
            log.info("Access Token 만료: {}", e.getMessage());
            request.setAttribute("exception", "ACCESS_TOKEN_EXPIRED");
            
        } catch (JwtException | IllegalArgumentException e) {
            // 그 외 토큰 오류 (서명 불일치, 구조 깨짐 등)
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            request.setAttribute("exception", "INVALID_TOKEN");
        }
    } 
    
    // 3. 토큰이 없는 경우 (accessToken == null)
    else {
        // 필요하다면 여기서 Refresh Token 존재 여부를 체크할 수도 있지만,
        // 보통은 그냥 비워두고 401 에러가 나면 클라이언트가 재발급 요청을 보내도록 유도합니다.
        request.setAttribute("exception", "MISSING_TOKEN"); 
    }

    // 4. 다음 필터로 진행
    // (SecurityContext가 비어있으면 뒤쪽의 AuthorizationFilter나 CustomEntryPoint에서 처리)
    filterChain.doFilter(request, response);
}

  /**
   * 요청 쿠키에서 JWT 토큰 추출
   */
  private String resolveAccessToken(@NonNull HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, "accessToken");
    if (cookie != null) {
      return cookie.getValue();
    }
    return null;
  }

  private String resolveRefreshToken(@NonNull HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, "refreshToken");
    if (cookie != null) {
      return cookie.getValue();
    }
    return null;
  }
}
