package com.sns.marigold.auth.common.jwt;

import com.sns.marigold.auth.common.CustomPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtManager {

  public static final String AUTHORITIES_KEY = "auth";
  public static final String USER_ID_KEY = "sub"; // subject

  private final SecretKey secretKey;
  @Getter
  public final long accessTokenValidityInMilliseconds;
  @Getter
  public final long refreshTokenValidityInMilliseconds;

  public JwtManager(
      @Value("${jwt.secret.defaultSecretKeyForDevelopmentOnlyChangeInProduction}") String secret,
      @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
      @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
    this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
  }

  /*
    토큰 payload 정보를 바탕으로 Authentication 객체를 생성하여 반환
   */
  public Authentication getAuthentication(String token) {
    Claims claims = getClaims(token);
    UUID userId = getUserId(claims);
    List<SimpleGrantedAuthority> authorities = getAuthorities(claims);
    return new UsernamePasswordAuthenticationToken(new CustomPrincipal(userId, authorities, null), "", authorities);
  }

  /**
   * Access Token 생성
   */
  public String createAccessToken(CustomPrincipal principal) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    List<String> authorities = principal.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toList());

    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .claim(USER_ID_KEY, principal.getUserId().toString()) // 사용자 ID
        .claim(AUTHORITIES_KEY, authorities) // 권한 정보
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
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Token에서 Claims 추출
   */
  public Claims getClaims(String token) {
    try {
      return Jwts.parser()
      .verifyWith(secretKey)
      .build()
      .parseSignedClaims(token)
      .getPayload();
    } catch (ExpiredJwtException e) {
      // 만료된 토큰이어도 Claims 정보(ID 등)가 필요할 때가 있어서 반환해주는 경우도 있지만,
      // 기본 인증 과정에서는 예외를 그대로 던져서 Filter가 잡게 하는 것이 정석입니다.
      throw e;
    } catch (JwtException e) {
      throw new JwtException("Invalid JWT token", e);
    }
  }

  /**
   * Token에서 User ID 추출
   */
  public UUID getUserId(Claims claims) {
    return UUID.fromString(claims.get(USER_ID_KEY, String.class));
  }

  /**
   * Token에서 권한 정보 추출
   */
  @SuppressWarnings("unchecked")
  public List<SimpleGrantedAuthority> getAuthorities(Claims claims) {
    List<String> authorities = claims.get(AUTHORITIES_KEY, List.class);
    if (authorities == null) {
      return List.of();
    }
    return authorities.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }

  public Long getAccessTokenValidityInSeconds() {
    return this.accessTokenValidityInMilliseconds / 1000;
  }

  public Long getRefreshTokenValidityInSeconds() {
    return this.refreshTokenValidityInMilliseconds / 1000;
  }
}
