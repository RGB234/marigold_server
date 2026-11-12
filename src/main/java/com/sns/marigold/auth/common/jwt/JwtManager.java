package com.sns.marigold.auth.common.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.lang.Nullable;
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
  public static final String SESSION_ID_KEY = "sid";

  private final SecretKey accessKey;
  @Getter public final long accessTtlInMilliseconds;

  public JwtManager(JwtProperties jwtProperties) {
    this.accessKey =
        Keys.hmacShaKeyFor(jwtProperties.access().secret().getBytes(StandardCharsets.UTF_8));
    this.accessTtlInMilliseconds = jwtProperties.access().ttlSeconds() * 1000;
  }

  /** Access Token 생성. 요청마다 sid의 세션이 살아 있는지 확인한다 */
  public String createAccessToken(CustomPrincipal principal, String sessionId) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTtlInMilliseconds);

    List<String> authorities =
        principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

    return Jwts.builder()
        .subject(principal.getUserId().toString())
        .claim(AUTHORITIES_KEY, authorities) // 권한 정보
        .claim(TOKEN_USE_KEY, ACCESS_TOKEN_USE)
        .claim(SESSION_ID_KEY, sessionId)
        .issuedAt(now)
        .expiration(validity)
        .signWith(accessKey)
        .compact();
  }

  public Claims getAccessClaims(String token) {
    return parseClaims(token, accessKey);
  }

  /** Token에서 User ID 추출 */
  public Long getUserId(Claims claims) {
    return Long.parseLong(claims.get("sub", String.class));
  }

  /** 인증 세션 ID. 세션 도입 전에 발급된 token에는 없다 */
  @Nullable
  public String getSessionId(Claims claims) {
    return claims.get(SESSION_ID_KEY, String.class);
  }

  public boolean isAccessToken(Claims claims) {
    return ACCESS_TOKEN_USE.equals(claims.get(TOKEN_USE_KEY, String.class));
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

  private Claims parseClaims(String token, SecretKey key) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
