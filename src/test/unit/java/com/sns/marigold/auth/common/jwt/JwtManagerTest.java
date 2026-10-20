package com.sns.marigold.auth.common.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

class JwtManagerTest {

  private static final String TEST_ACCESS_SECRET = "a".repeat(64);
  private static final String TEST_REFRESH_SECRET = "b".repeat(64);

  private JwtManager jwtManager;
  private CustomPrincipal testPrincipal;

  @BeforeEach
  void setUp() {
    // 256bit 이상의 비밀키 (32 bytes)
    jwtManager =
        new JwtManager(
            new JwtProperties(
                new JwtProperties.Token(TEST_ACCESS_SECRET, 600),
                new JwtProperties.Token(TEST_REFRESH_SECRET, 86400)));

    List<SimpleGrantedAuthority> authorities =
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PERSON"));
    testPrincipal = new CustomPrincipal(1L, authorities, Map.of(), AuthStatus.LOGIN_SUCCESS);
  }

  @Test
  @DisplayName("Access Token 정상 생성 및 파싱")
  void createAndParseAccessToken() {
    // when
    String token = jwtManager.createAccessToken(testPrincipal);

    // then
    assertThat(token).isNotBlank();
    assertThat(jwtManager.getAccessTtlInMilliseconds()).isEqualTo(600_000L);

    Claims claims = jwtManager.getAccessClaims(token);
    assertThat(claims.getSubject()).isEqualTo("1");
    assertThat(claims.get("sub")).isEqualTo("1");
    assertThat(jwtManager.isAccessToken(claims)).isTrue();
    assertThat(jwtManager.isRefreshToken(claims)).isFalse();

    List<SimpleGrantedAuthority> authorities = jwtManager.getAuthorities(claims);
    assertThat(authorities).hasSize(1);
    assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_PERSON");
  }

  @Test
  @DisplayName("Refresh Token 정상 생성 및 파싱")
  void createAndParseRefreshToken() {
    // when
    String token = jwtManager.createRefreshToken(testPrincipal);

    // then
    assertThat(token).isNotBlank();

    Claims claims = jwtManager.getRefreshClaims(token);
    assertThat(claims.getSubject()).isEqualTo("1");
    assertThat(claims.getId()).isNotBlank();
    assertThat(jwtManager.isRefreshToken(claims)).isTrue();
    assertThat(jwtManager.isAccessToken(claims)).isFalse();
    // Refresh token should not have authorities
    assertThat(claims.get(JwtManager.AUTHORITIES_KEY)).isNull();
  }

  @Test
  void refreshTokens_AreUniquePerIssue() {
    assertThat(jwtManager.createRefreshToken(testPrincipal))
        .isNotEqualTo(jwtManager.createRefreshToken(testPrincipal));
  }

  @Test
  @DisplayName("잘못된 형식의 토큰 파싱 시 예외 발생")
  void parseInvalidToken() {
    // given
    String invalidToken = "invalid.token.string";

    // when & then
    assertThatThrownBy(() -> jwtManager.getAccessClaims(invalidToken))
        .isInstanceOf(MalformedJwtException.class);
  }

  @Test
  @DisplayName("만료된 토큰 파싱 시 예외 발생")
  void parseExpiredToken() throws InterruptedException {
    // given
    // 유효시간이 0인 JwtManager 생성 (테스트용)
    JwtManager expiredJwtManager =
        new JwtManager(
            new JwtProperties(
                new JwtProperties.Token(TEST_ACCESS_SECRET, 0),
                new JwtProperties.Token(TEST_REFRESH_SECRET, 0)));
    String token = expiredJwtManager.createAccessToken(testPrincipal);

    // when & then
    Thread.sleep(10); // 확실하게 만료시키기 위해 약간의 대기
    assertThatThrownBy(() -> expiredJwtManager.getAccessClaims(token))
        .isInstanceOf(ExpiredJwtException.class);
  }
}
