package com.sns.marigold.auth.common.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JwtManagerTest {

  private JwtManager jwtManager;
  private CustomPrincipal testPrincipal;

  @BeforeEach
  void setUp() {
    // 256bit 이상의 비밀키 (32 bytes)
    String secret = "this-is-a-very-long-secret-key-for-testing-purpose-only";
    jwtManager = new JwtManager(secret, 3600, 86400);

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

    Claims claims = jwtManager.getClaims(token);
    assertThat(claims.getSubject()).isEqualTo("1");
    assertThat(claims.get(JwtManager.USER_ID_KEY)).isEqualTo("1");

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

    Claims claims = jwtManager.getClaims(token);
    assertThat(claims.getSubject()).isEqualTo("1");
    // Refresh token should not have authorities
    assertThat(claims.get(JwtManager.AUTHORITIES_KEY)).isNull();
  }

  @Test
  @DisplayName("토큰으로부터 Authentication 객체 생성")
  void getAuthentication() {
    // given
    String token = jwtManager.createAccessToken(testPrincipal);

    // when
    Authentication authentication = jwtManager.getAuthentication(token);

    // then
    assertThat(authentication.getPrincipal()).isInstanceOf(CustomPrincipal.class);
    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
    assertThat(principal.getUserId()).isEqualTo(1L);
    assertThat(authentication.getAuthorities()).hasSize(1);
  }

  @Test
  @DisplayName("잘못된 형식의 토큰 파싱 시 예외 발생")
  void parseInvalidToken() {
    // given
    String invalidToken = "invalid.token.string";

    // when & then
    assertThatThrownBy(() -> jwtManager.getClaims(invalidToken))
        .isInstanceOf(MalformedJwtException.class);
  }

  @Test
  @DisplayName("만료된 토큰 파싱 시 예외 발생")
  void parseExpiredToken() throws InterruptedException {
    // given
    // 유효시간이 0인 JwtManager 생성 (테스트용)
    JwtManager expiredJwtManager =
        new JwtManager("this-is-a-very-long-secret-key-for-testing-purpose-only", 0, 0);
    String token = expiredJwtManager.createAccessToken(testPrincipal);

    // when & then
    Thread.sleep(10); // 확실하게 만료시키기 위해 약간의 대기
    assertThatThrownBy(() -> expiredJwtManager.getClaims(token))
        .isInstanceOf(ExpiredJwtException.class);
  }
}
