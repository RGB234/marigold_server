package com.sns.marigold.auth.common.jwt;

import com.sns.marigold.auth.common.CustomPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
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

  private final SecretKey key;
  @Getter public final long accessTokenValidityInMilliseconds;
  @Getter public final long refreshTokenValidityInMilliseconds;

  public JwtManager(
      @Value("${jwt.secret.defaultSecretKeyForDevelopmentOnlyChangeInProduction}") String secret,
      @Value("${jwt.access-token-validity-in-seconds}") long accessTokenValidityInSeconds,
      @Value("${jwt.refresh-token-validity-in-seconds}") long refreshTokenValidityInSeconds) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
    this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
  }

  /*
   * 토큰 payload 정보를 바탕으로 Authentication 객체를 생성하여 반환
   */
  public Authentication getAuthentication(String token) {
    // 토큰이 유효한 경우 Claims 추출. 유효하지 않은 경우 예외 발생
    Claims claims = getClaims(token);
    Long userId = getUserId(claims);
    List<SimpleGrantedAuthority> authorities = getAuthorities(claims);
    return new UsernamePasswordAuthenticationToken(
        new CustomPrincipal(userId, authorities, null, null), "", authorities);
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
        .claim(USER_ID_KEY, principal.getUserId().toString()) // 사용자 ID
        .claim(AUTHORITIES_KEY, authorities) // 권한 정보
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
        .claim(USER_ID_KEY, principal.getUserId().toString())
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
    return Long.parseLong(claims.get(USER_ID_KEY, String.class));
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

  public Long getAccessTokenValidityInSeconds() {
    return this.accessTokenValidityInMilliseconds / 1000;
  }

  public Long getRefreshTokenValidityInSeconds() {
    return this.refreshTokenValidityInMilliseconds / 1000;
  }
}
