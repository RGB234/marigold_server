package com.sns.marigold.auth.common.jwt;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
      try {
        Authentication authentication = getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (Exception e) {
        log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
        SecurityContextHolder.clearContext();
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * 요청 헤더에서 JWT 토큰 추출
   */
  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  /**
   * JWT 토큰으로부터 Authentication 객체 생성
   */
  private Authentication getAuthentication(String token) {
    UUID userId = jwtTokenProvider.getUserId(token);
    List<SimpleGrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token);
    ProviderInfo providerInfo = jwtTokenProvider.getProviderInfo(token);

    // JWT 인증을 위한 최소한의 OAuth2UserInfo 생성
    OAuth2UserInfo oAuth2UserInfo = createMinimalOAuth2UserInfo(providerInfo);

    CustomPrincipal principal = new CustomPrincipal(userId, oAuth2UserInfo, authorities);

    return new UsernamePasswordAuthenticationToken(principal, token, authorities);
  }

  /**
   * JWT 인증을 위한 최소한의 OAuth2UserInfo 생성
   */
  private OAuth2UserInfo createMinimalOAuth2UserInfo(ProviderInfo providerInfo) {
    return new OAuth2UserInfo(Collections.emptyMap(), providerInfo) {
      @Override
      public String getName() {
        return "";
      }

      @Override
      public String getEmail() {
        return "";
      }
    };
  }
}

