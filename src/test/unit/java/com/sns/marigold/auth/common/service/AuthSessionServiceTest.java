package com.sns.marigold.auth.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.common.service.AuthSessionService.RotatedSession;
import com.sns.marigold.auth.common.service.AuthSessionService.VerifiedSession;
import com.sns.marigold.auth.common.session.AuthSessionProperties;
import com.sns.marigold.auth.common.session.AuthSessionStore;
import com.sns.marigold.auth.common.session.AuthSessionStore.Rotation;
import com.sns.marigold.auth.common.session.RefreshToken;
import com.sns.marigold.auth.common.session.SessionTokens;
import com.sns.marigold.auth.event.AuthSessionsRevokedEvent;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.auth.exception.AuthException;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

  private static final long REFRESH_TTL_SECONDS = 86_400;
  private static final long ABSOLUTE_TTL_SECONDS = 1_209_600;

  @Mock private AuthSessionStore sessionStore;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private AuditLogger auditLogger;

  private JwtManager jwtManager;
  private CustomPrincipal principal;

  @BeforeEach
  void setUp() {
    jwtManager = new JwtManager(new JwtProperties(new JwtProperties.Token("a".repeat(64), 600)));
    principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority("ROLE_PERSON")),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
  }

  private AuthSessionService service(long absoluteTtlSeconds) {
    return new AuthSessionService(
        sessionStore,
        jwtManager,
        new AuthSessionProperties(REFRESH_TTL_SECONDS, absoluteTtlSeconds, 10),
        eventPublisher,
        auditLogger);
  }

  @Test
  @DisplayName("로그인하면 비밀값의 해시만 세션에 저장하고, 두 token에 같은 세션 ID를 담는다.")
  void start_StoresOnlyTheSecretHash() {
    SessionTokens tokens = service(ABSOLUTE_TTL_SECONDS).start(principal);

    ArgumentCaptor<String> sessionId = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> tokenHash = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Instant> authenticatedAt = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> expiresAt = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> absoluteExpiresAt = ArgumentCaptor.forClass(Instant.class);
    verify(sessionStore)
        .create(
            sessionId.capture(),
            eq(1L),
            tokenHash.capture(),
            authenticatedAt.capture(),
            expiresAt.capture(),
            absoluteExpiresAt.capture());

    RefreshToken refreshToken = RefreshToken.parse(tokens.refreshToken());
    assertThat(refreshToken).isNotNull();
    assertThat(refreshToken.sessionId()).isEqualTo(sessionId.getValue());
    assertThat(refreshToken.secretHash()).isEqualTo(tokenHash.getValue());
    assertThat(tokens.refreshToken()).doesNotContain(tokenHash.getValue());
    assertThat(jwtManager.getSessionId(jwtManager.getAccessClaims(tokens.accessToken())))
        .isEqualTo(sessionId.getValue());
    assertThat(Duration.between(authenticatedAt.getValue(), expiresAt.getValue()))
        .isEqualTo(Duration.ofSeconds(REFRESH_TTL_SECONDS));
    assertThat(Duration.between(expiresAt.getValue(), absoluteExpiresAt.getValue()))
        .isEqualTo(Duration.ofSeconds(ABSOLUTE_TTL_SECONDS - REFRESH_TTL_SECONDS));
    assertThat(tokens.refreshMaxAgeSeconds()).isEqualTo(REFRESH_TTL_SECONDS);
  }

  @Test
  @DisplayName("절대 만료가 refresh TTL보다 짧으면 쿠키도 절대 만료에 맞춘다.")
  void start_CapsRefreshExpiryAtAbsoluteExpiry() {
    SessionTokens tokens = service(60).start(principal);

    ArgumentCaptor<Instant> expiresAt = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> absoluteExpiresAt = ArgumentCaptor.forClass(Instant.class);
    verify(sessionStore)
        .create(
            anyString(),
            eq(1L),
            anyString(),
            any(Instant.class),
            expiresAt.capture(),
            absoluteExpiresAt.capture());
    assertThat(expiresAt.getValue()).isEqualTo(absoluteExpiresAt.getValue());
    assertThat(tokens.refreshMaxAgeSeconds()).isEqualTo(60L);
  }

  @Test
  @DisplayName("현재 refresh token이면 같은 세션의 새 값으로 바꾸고 세션의 사용자를 알려 준다.")
  void rotate_IssuesNewValueForTheSameSession() {
    RefreshToken presented = RefreshToken.issue("session-1");
    Instant expiresAt = Instant.now().plusSeconds(3_600);
    given(
            sessionStore.rotate(
                eq("session-1"), eq(presented.secretHash()), anyString(), any(), any(), any()))
        .willReturn(new Rotation(Rotation.Status.ROTATED, 7L, expiresAt));

    RotatedSession rotated = service(ABSOLUTE_TTL_SECONDS).rotate(presented.value());

    RefreshToken next = RefreshToken.parse(rotated.refreshToken());
    assertThat(next).isNotNull();
    assertThat(next.sessionId()).isEqualTo("session-1");
    assertThat(next.secret()).isNotEqualTo(presented.secret());
    assertThat(rotated.userId()).isEqualTo(7L);
    assertThat(rotated.sessionId()).isEqualTo("session-1");
    assertThat(rotated.refreshMaxAgeSeconds()).isBetween(3_599L, 3_600L);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("이전 refresh token이 다시 오면 세션 폐기를 알리고 거부한다.")
  void rotate_RejectsReusedTokenAndAnnouncesRevocation() {
    RefreshToken presented = RefreshToken.issue("session-1");
    given(sessionStore.rotate(eq("session-1"), anyString(), anyString(), any(), any(), any()))
        .willReturn(new Rotation(Rotation.Status.REUSED, 7L, null));
    AuthSessionService service = service(ABSOLUTE_TTL_SECONDS);

    assertThatThrownBy(() -> service.rotate(presented.value()))
        .isInstanceOfSatisfying(
            AuthException.class,
            e -> assertThat(e.getErrorSpec()).isEqualTo(AuthError.TOKEN_INVALID));
    verify(eventPublisher).publishEvent(new AuthSessionsRevokedEvent(List.of("session-1")));
  }

  @Test
  @DisplayName("세션이 없으면(로그아웃·만료) 거부한다.")
  void rotate_RejectsMissingSession() {
    RefreshToken presented = RefreshToken.issue("session-1");
    given(sessionStore.rotate(eq("session-1"), anyString(), anyString(), any(), any(), any()))
        .willReturn(new Rotation(Rotation.Status.MISSING, null, null));
    AuthSessionService service = service(ABSOLUTE_TTL_SECONDS);

    assertThatThrownBy(() -> service.rotate(presented.value())).isInstanceOf(AuthException.class);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("형식이 맞지 않는 값(이전 JWT refresh token 포함)은 저장소를 보지 않고 거부한다.")
  void rotate_RejectsMalformedValueWithoutStoreLookup() {
    AuthSessionService service = service(ABSOLUTE_TTL_SECONDS);

    assertThatThrownBy(() -> service.rotate("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2ln"))
        .isInstanceOf(AuthException.class);
    verifyNoInteractions(sessionStore);
  }

  @Test
  @DisplayName("현재 refresh token이면 세션과 사용자를 돌려준다.")
  void verify_ReturnsSessionOfCurrentToken() {
    RefreshToken token = RefreshToken.issue("session-1");
    given(sessionStore.findUserId("session-1", token.secretHash())).willReturn(7L);

    VerifiedSession session = service(ABSOLUTE_TTL_SECONDS).verify(token.value());

    assertThat(session).isEqualTo(new VerifiedSession(7L, "session-1"));
  }

  @Test
  @DisplayName("로그아웃하면 현재 refresh token의 세션을 지우고 폐기를 알린다.")
  void end_DeletesSessionOfCurrentToken() {
    RefreshToken token = RefreshToken.issue("session-1");
    given(sessionStore.findUserId("session-1", token.secretHash())).willReturn(7L);

    service(ABSOLUTE_TTL_SECONDS).end(token.value());

    verify(sessionStore).delete("session-1");
    verify(eventPublisher).publishEvent(new AuthSessionsRevokedEvent(List.of("session-1")));
  }

  @Test
  @DisplayName("현재 token이 아니면 로그아웃해도 세션을 지우지 않는다.")
  void end_IgnoresTokenThatIsNotCurrent() {
    RefreshToken token = RefreshToken.issue("session-1");
    // Mockito는 Long 반환을 null이 아닌 0으로 채우므로 "현재 token 아님"을 명시한다
    given(sessionStore.findUserId("session-1", token.secretHash())).willReturn(null);

    service(ABSOLUTE_TTL_SECONDS).end(token.value());
    service(ABSOLUTE_TTL_SECONDS).end("not-a-refresh-token");

    verify(sessionStore, never()).delete(anyString());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("다시 로그인하면 이전 refresh token의 세션을 지우고 폐기를 알린다.")
  void endPrevious_DeletesSessionOfPreviousToken() {
    RefreshToken token = RefreshToken.issue("session-1");
    given(sessionStore.findUserId("session-1", token.secretHash())).willReturn(7L);

    service(ABSOLUTE_TTL_SECONDS).endPrevious(token.value());

    verify(sessionStore).delete("session-1");
    verify(eventPublisher).publishEvent(new AuthSessionsRevokedEvent(List.of("session-1")));
  }

  @Test
  @DisplayName("이전 세션을 지우다 저장소가 실패해도 로그인을 막지 않도록 예외를 던지지 않는다.")
  void endPrevious_IgnoresStoreFailure() {
    RefreshToken token = RefreshToken.issue("session-1");
    given(sessionStore.findUserId("session-1", token.secretHash()))
        .willThrow(AuthException.forSessionStoreUnavailable(new IllegalStateException("down")));
    AuthSessionService service = service(ABSOLUTE_TTL_SECONDS);

    assertThatCode(() -> service.endPrevious(token.value())).doesNotThrowAnyException();
    verifyNoInteractions(eventPublisher);
  }

  @Test
  @DisplayName("사용자의 모든 세션을 지우고, 지운 세션이 있을 때만 폐기를 알린다.")
  void endAll_AnnouncesOnlyDeletedSessions() {
    given(sessionStore.deleteAll(1L)).willReturn(List.of("a", "b"));
    given(sessionStore.deleteAll(2L)).willReturn(List.of());
    AuthSessionService service = service(ABSOLUTE_TTL_SECONDS);

    service.endAll(1L);
    service.endAll(2L);

    verify(eventPublisher).publishEvent(new AuthSessionsRevokedEvent(List.of("a", "b")));
    verify(eventPublisher, never()).publishEvent(new AuthSessionsRevokedEvent(List.of()));
  }
}
