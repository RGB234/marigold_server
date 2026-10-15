package com.sns.marigold.auth.common.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.sns.marigold.auth.common.CustomPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;

@Component
public class JwtManager {

  public static final String AUTHORITIES_KEY = "auth";
  public static final String TOKEN_USE_KEY = "token_use";
  public static final String ACCESS_TOKEN_USE = "access";
  public static final String REFRESH_TOKEN_USE = "refresh";

  private final SecretKey key;
  @Getter public final long accessTokenValidityInMilliseconds;
  @Getter public final long refreshTokenValidityInMilliseconds;

  public JwtManager(JwtProperties jwtProperties) {
    this.key = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = jwtProperties.accessTokenValidityInSeconds() * 1000;
    this.refreshTokenValidityInMilliseconds = jwtProperties.refreshTokenValidityInSeconds() * 1000;
  }

  /** Access Token 생성 */
  public String createAccessToken(CustomPrincipal principal) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    List<String> authorities =
        principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .claim(AUTHORITIES_KEY, authorities) // 권한 정보
        .claim(TOKEN_USE_KEY, ACCESS_TOKEN_USE)
        .issuedAt(now)
        .expiration(validity)
        .signWith(key)
        .compact();
  }

  /** Refresh Token 생성 */
  public String createRefreshToken(CustomPrincipal principal) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .id(UUID.randomUUID().toString())
        .claim(TOKEN_USE_KEY, REFRESH_TOKEN_USE)
        .issuedAt(now)
        .expiration(validity)
        .signWith(key)
        .compact();
  }

  /** Token에서 Claims 추출 */
  public Claims getClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  /** Token에서 User ID 추출 */
  public Long getUserId(Claims claims) {
    return Long.parseLong(claims.get("sub", String.class));
  }

  public boolean isAccessToken(Claims claims) {
    return ACCESS_TOKEN_USE.equals(claims.get(TOKEN_USE_KEY, String.class));
  }

  public boolean isRefreshToken(Claims claims) {
    return REFRESH_TOKEN_USE.equals(claims.get(TOKEN_USE_KEY, String.class));
  }

  /** Token에서 권한 정보 추출 */
  @SuppressWarnings("unchecked")
  public List<SimpleGrantedAuthority> getAuthorities(Claims claims) {
    List<String> authorities = claims.get(AUTHORITIES_KEY, List.class);
    if (authorities == null) {
      return List.of();
    }
    return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }

  public Long getRefreshTokenValidityInSeconds() {
    return this.refreshTokenValidityInMilliseconds / 1000;
  }
}
