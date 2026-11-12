package com.sns.marigold.auth.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.auth.common.AccessTokenDetails;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.auth.exception.AuthException;

import io.jsonwebtoken.JwtException;

class JwtAuthenticationServiceTest {

  private static final String TEST_ACCESS_SECRET = "a".repeat(64);

  private JwtManager jwtManager;
  private AuthSessionService authSessionService;
  private JwtAuthenticationService jwtAuthenticationService;
  private CustomPrincipal principal;

  @BeforeEach
  void setUp() {
    jwtManager =
        new JwtManager(new JwtProperties(new JwtProperties.Token(TEST_ACCESS_SECRET, 600)));
    authSessionService = mock(AuthSessionService.class);
    jwtAuthenticationService = new JwtAuthenticationService(jwtManager, authSessionService);
    principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority("ROLE_PERSON")),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
  }

  @Test
  @DisplayName("세션이 살아 있으면 access token의 claim으로 인증 객체를 생성한다.")
  void getAuthentication_CreatesAuthenticationFromAccessTokenClaims() {
    String accessToken = jwtManager.createAccessToken(principal, "session-1");
    given(authSessionService.isActive("session-1")).willReturn(true);

    Authentication authentication = jwtAuthenticationService.getAuthentication(accessToken);

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getDetails())
        .isInstanceOfSatisfying(
            AccessTokenDetails.class,
            details -> assertThat(details.sessionId()).isEqualTo("session-1"));
    assertThat(authentication.getCredentials()).isEqualTo(accessToken);
    assertThat(((CustomPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(1L);
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_PERSON");
  }

  @Test
  @DisplayName("다른 키로 서명한 token은 요청 인증에 사용할 수 없다.")
  void getAuthentication_RejectsTokenSignedWithAnotherKey() {
    JwtManager otherManager =
        new JwtManager(new JwtProperties(new JwtProperties.Token("c".repeat(64), 600)));
    String forged = otherManager.createAccessToken(principal, "session-1");

    assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(forged))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("로그아웃 등으로 폐기된 세션의 access token은 만료 전이라도 거부한다.")
  void getAuthentication_RejectsRevokedSession() {
    String accessToken = jwtManager.createAccessToken(principal, "revoked");
    given(authSessionService.isActive("revoked")).willReturn(false);

    assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
        .isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getErrorSpec()).isEqualTo(AuthError.TOKEN_INVALID));
  }

  @Test
  @DisplayName("세션 저장소가 응답하지 않으면 401이 아닌 503 오류를 그대로 전달한다.")
  void getAuthentication_PropagatesSessionStoreFailure() {
    String accessToken = jwtManager.createAccessToken(principal, "session-1");
    given(authSessionService.isActive("session-1"))
        .willThrow(AuthException.forSessionStoreUnavailable(new IllegalStateException("down")));

    assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
        .isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getErrorSpec()).isEqualTo(AuthError.SESSION_STORE_UNAVAILABLE));
  }
}
