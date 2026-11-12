package com.sns.marigold.auth.common.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.sns.marigold.auth.common.AccessTokenDetails;
import com.sns.marigold.auth.common.recent.RecentAuthProperties;
import com.sns.marigold.auth.common.session.AuthSessionStore;
import com.sns.marigold.auth.exception.AuthException;

@ExtendWith(MockitoExtension.class)
class RecentAuthServiceTest {

  @Mock private AuthSessionStore sessionStore;

  private RecentAuthService recentAuthService;

  @BeforeEach
  void setUp() {
    recentAuthService = new RecentAuthService(sessionStore, new RecentAuthProperties(300L));
  }

  @Test
  void validate_PassesWhenSessionLoggedInRecently() {
    given(sessionStore.getAuthenticatedAt("session-1")).willReturn(Instant.now().minusSeconds(60));

    assertThatCode(() -> recentAuthService.validate(authentication("session-1")))
        .doesNotThrowAnyException();
  }

  @Test
  void validate_RejectsSessionLoggedInLongAgo() {
    given(sessionStore.getAuthenticatedAt("session-1")).willReturn(Instant.now().minusSeconds(301));

    assertThatThrownBy(() -> recentAuthService.validate("session-1"))
        .isInstanceOf(AuthException.class);
  }

  @Test
  void validate_RejectsMissingSession() {
    assertThatThrownBy(() -> recentAuthService.validate("session-1"))
        .isInstanceOf(AuthException.class);
  }

  @Test
  void validate_RejectsAuthenticationWithoutAccessTokenDetails() {
    assertThatThrownBy(
            () ->
                recentAuthService.validate(
                    new UsernamePasswordAuthenticationToken("user", null, List.of())))
        .isInstanceOf(AuthException.class);
  }

  private UsernamePasswordAuthenticationToken authentication(String sessionId) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken("user", null, List.of());
    authentication.setDetails(new AccessTokenDetails(sessionId, Instant.now().plusSeconds(600)));
    return authentication;
  }
}
