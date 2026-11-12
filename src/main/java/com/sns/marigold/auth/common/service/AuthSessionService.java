package com.sns.marigold.auth.common.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.session.AuthSessionProperties;
import com.sns.marigold.auth.common.session.AuthSessionStore;
import com.sns.marigold.auth.common.session.AuthSessionStore.Rotation;
import com.sns.marigold.auth.common.session.RefreshToken;
import com.sns.marigold.auth.common.session.SessionTokens;
import com.sns.marigold.auth.event.AuthSessionsRevokedEvent;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.error.exception.ApplicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인 한 번을 Redis 세션 하나로 관리합니다. access token(JWT)과 refresh token(불투명 값)은 모두 세션 ID(sid)를 담고, 세션이 지워지면
 * 두 token 모두 바로 쓸 수 없게 됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthSessionService {

  private final AuthSessionStore sessionStore;
  private final JwtManager jwtManager;
  private final AuthSessionProperties sessionProperties;
  private final ApplicationEventPublisher eventPublisher;
  private final AuditLogger auditLogger;

  public SessionTokens start(CustomPrincipal principal) {
    Instant now = Instant.now();
    RefreshToken refreshToken = RefreshToken.issue(UUID.randomUUID().toString());
    Instant absoluteExpiresAt = now.plusSeconds(sessionProperties.absoluteTtlSeconds());
    Instant expiresAt =
        min(now.plusSeconds(sessionProperties.refreshTtlSeconds()), absoluteExpiresAt);

    sessionStore.create(
        refreshToken.sessionId(),
        principal.getUserId(),
        refreshToken.secretHash(),
        now,
        expiresAt,
        absoluteExpiresAt);
    return new SessionTokens(
        createAccessToken(principal, refreshToken.sessionId()),
        refreshToken.value(),
        secondsBetween(now, expiresAt));
  }

  /**
   * refresh token을 새 값으로 바꿉니다. access token은 호출한 쪽이 DB에서 현재 사용자 상태와 권한을 확인한 뒤 {@link
   * #createAccessToken}으로 발급합니다.
   */
  public RotatedSession rotate(String refreshToken) {
    RefreshToken presented = RefreshToken.parse(refreshToken);
    if (presented == null) {
      throw AuthException.forInvalidToken();
    }

    Instant now = Instant.now();
    RefreshToken next = RefreshToken.issue(presented.sessionId());
    Rotation rotation =
        sessionStore.rotate(
            presented.sessionId(),
            presented.secretHash(),
            next.secretHash(),
            now,
            Duration.ofSeconds(sessionProperties.refreshTtlSeconds()),
            Duration.ofSeconds(sessionProperties.reuseGraceSeconds()));

    return switch (rotation.status()) {
      case ROTATED ->
          new RotatedSession(
              rotation.userId(),
              presented.sessionId(),
              next.value(),
              secondsBetween(now, rotation.expiresAt()));
      case MISSING -> throw AuthException.forInvalidToken();
      case REUSED -> {
        auditLogger.warn(
            "event=refresh_token_reuse_detected userId={} sessionId={}",
            rotation.userId(),
            presented.sessionId());
        eventPublisher.publishEvent(new AuthSessionsRevokedEvent(List.of(presented.sessionId())));
        throw AuthException.forInvalidToken();
      }
    };
  }

  public String createAccessToken(CustomPrincipal principal, String sessionId) {
    return jwtManager.createAccessToken(principal, sessionId);
  }

  public boolean isActive(String sessionId) {
    return sessionStore.exists(sessionId);
  }

  /** refresh token이 살아 있는 세션의 현재 token이면 그 세션을, 아니면 null을 돌려줍니다. */
  @Nullable
  public VerifiedSession verify(String refreshToken) {
    RefreshToken presented = RefreshToken.parse(refreshToken);
    if (presented == null) {
      return null;
    }
    Long userId = sessionStore.findUserId(presented.sessionId(), presented.secretHash());
    return userId == null ? null : new VerifiedSession(userId, presented.sessionId());
  }

  /** 로그아웃. 현재 token이 아니면(만료, 위조, 이미 폐기) 지울 세션이 없으므로 아무것도 하지 않습니다. */
  public void end(String refreshToken) {
    VerifiedSession session = verify(refreshToken);
    if (session != null) {
      revoke(session.sessionId());
    }
  }

  /**
   * 다시 로그인한 브라우저가 들고 있던 이전 세션을 폐기합니다. 새 세션을 만든 뒤에 호출합니다. 저장소가 응답하지 않아도 로그인을 막지 않으며, 남은 세션은 만료 시각에
   * 사라집니다.
   */
  public void endPrevious(String refreshToken) {
    try {
      end(refreshToken);
    } catch (ApplicationException e) {
      log.warn("Failed to revoke previous auth session on login: code={}", e.getErrorSpec().code());
    }
  }

  public void revoke(String sessionId) {
    sessionStore.delete(sessionId);
    eventPublisher.publishEvent(new AuthSessionsRevokedEvent(List.of(sessionId)));
  }

  public void endAll(Long userId) {
    List<String> sessionIds = sessionStore.deleteAll(userId);
    if (!sessionIds.isEmpty()) {
      eventPublisher.publishEvent(new AuthSessionsRevokedEvent(sessionIds));
    }
  }

  private static long secondsBetween(Instant from, Instant to) {
    return Math.max(0, Duration.between(from, to).toSeconds());
  }

  private static Instant min(Instant a, Instant b) {
    return a.isBefore(b) ? a : b;
  }

  /**
   * @param refreshMaxAgeSeconds 새 refresh 쿠키 유효시간. 세션 만료 시각까지 남은 시간이다
   */
  public record RotatedSession(
      Long userId, String sessionId, String refreshToken, long refreshMaxAgeSeconds) {

    // refresh token 값이 로그에 남지 않게 한다
    @Override
    public String toString() {
      return "RotatedSession[userId=" + userId + ", sessionId=" + sessionId + "]";
    }
  }

  public record VerifiedSession(Long userId, String sessionId) {}
}
