package com.sns.marigold.auth.common.jwt;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtManager {

  private static final String AUTHORITIES_KEY = "auth";
  private static final String USER_ID_KEY = "sub"; // subject
  private static final String PROVIDER_INFO_KEY = "providerInfo";

  private final SecretKey secretKey;
  private final long accessTokenValidityInMilliseconds;
  private final long refreshTokenValidityInMilliseconds;

  public JwtManager(
      @Value("${jwt.secret.defaultSecretKeyForDevelopmentOnlyChangeInProduction}") String secret,
      @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
      @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
    this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
  }

  /**
   * Access Token 생성
   */
  public String createAccessToken(CustomPrincipal principal) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    List<String> authorities =
        principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .claim(USER_ID_KEY, principal.getUserId().toString()) // 사용자 ID
        .claim(AUTHORITIES_KEY, authorities) // 권한 정보
        .claim(PROVIDER_INFO_KEY, principal.getOAuth2UserInfo().getProviderInfo().name()) // 소셜 로그인 종류
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Refresh Token 생성
   */
  public String createRefreshToken(CustomPrincipal principal) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .claim(USER_ID_KEY, principal.getUserId().toString())
        .claim(PROVIDER_INFO_KEY, principal.getOAuth2UserInfo().getProviderInfo().name())
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Token에서 ProviderInfo 추출
   */
  public ProviderInfo getProviderInfo(String token) {
    Claims claims = getClaims(token);
    String providerInfoName = claims.get(PROVIDER_INFO_KEY, String.class);
    return ProviderInfo.fromString(providerInfoName);
  }

  /**
   * Token에서 Claims 추출
   */
  public Claims getClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Token에서 User ID 추출
   */
  public UUID getUserId(String token) {
    Claims claims = getClaims(token);
    return UUID.fromString(claims.get(USER_ID_KEY, String.class));
  }

  /**
   * Token에서 권한 정보 추출
   */
  @SuppressWarnings("unchecked")
  public List<SimpleGrantedAuthority> getAuthorities(String token) {
    Claims claims = getClaims(token);
    List<String> authorities = claims.get(AUTHORITIES_KEY, List.class);
    if (authorities == null) {
      return List.of();
    }
    return authorities.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }

  /**
   * Token 유효성 검증
   */
  public boolean validateToken(String token) {
    try {
      Claims claims = getClaims(token);
      return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Token 만료 시간 추출
   */
  public Date getExpirationDate(String token) {
    Claims claims = getClaims(token);
    return claims.getExpiration();
  }
}

