package com.sns.marigold.auth.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.exception.AuthException;

class JwtAuthenticationServiceTest {

  private static final String TEST_SECRET = "a".repeat(64);

  private JwtManager jwtManager;
  private JwtAuthenticationService jwtAuthenticationService;
  private CustomPrincipal principal;

  @BeforeEach
  void setUp() {
    jwtManager = new JwtManager(new JwtProperties(TEST_SECRET, 600, 86400));
    jwtAuthenticationService = new JwtAuthenticationService(jwtManager);
    principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority("ROLE_PERSON")),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
  }

  @Test
  @DisplayName("Access token의 claim만으로 인증 객체를 생성한다.")
  void getAuthentication_CreatesAuthenticationFromAccessTokenClaims() {
    String accessToken = jwtManager.createAccessToken(principal);

    Authentication authentication = jwtAuthenticationService.getAuthentication(accessToken);

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getCredentials()).isEqualTo(accessToken);
    assertThat(((CustomPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(1L);
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_PERSON");
  }

  @Test
  @DisplayName("Refresh token은 요청 인증에 사용할 수 없다.")
  void getAuthentication_RejectsRefreshToken() {
    String refreshToken = jwtManager.createRefreshToken(principal);

    assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(refreshToken))
        .isInstanceOf(AuthException.class);
  }
}
