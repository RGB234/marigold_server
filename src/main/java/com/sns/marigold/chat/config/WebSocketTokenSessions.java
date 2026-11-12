package com.sns.marigold.chat.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/** 인증 토큰 만료, 인증 세션 폐기, 방 나가기 때 수신 전용 연결까지 종료하고 종료 예약을 정리합니다. */
@Component
@Slf4j
public class WebSocketTokenSessions {
  private static final CloseStatus JWT_EXPIRED = new CloseStatus(4001, "JWT expired");
  private static final CloseStatus SESSION_REVOKED = new CloseStatus(4002, "Session revoked");
  private static final CloseStatus LEFT_ROOM = new CloseStatus(4003, "Left chat room");

  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

  public WebSocketTokenSessions() {
    scheduler.setRemoveOnCancelPolicy(true);
  }

  void register(WebSocketSession socket) {
    sessions.put(socket.getId(), new Session(socket));
  }

  /** CONNECT에서 확인한 인증 세션을 기록하고 access token 만료 시각에 연결 종료를 예약합니다. */
  void expireAt(String sessionId, String authSessionId, Instant expiresAt) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      throw new AccessDeniedException("WebSocket session is closed");
    }
    synchronized (session) {
      if (sessions.get(sessionId) != session || session.expiration != null) {
        throw new AccessDeniedException("Invalid CONNECT session");
      }
      session.authSessionId = authSessionId;
      long delay = Math.max(0, expiresAt.toEpochMilli() - System.currentTimeMillis());
      session.expiration = scheduler.schedule(() -> close(sessionId), delay, TimeUnit.MILLISECONDS);
    }
  }

  /** 방 구독을 기록합니다. 참여 확인보다 먼저 기록해야, 확인과 나가기 커밋이 겹쳐도 {@link #closeRoom}이 이 세션을 찾습니다. */
  void subscribe(String sessionId, Long userId, Long roomId) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      throw new AccessDeniedException("WebSocket session is closed");
    }
    session.userId = userId;
    session.roomIds.add(roomId);
  }

  /** 방을 나간 사용자의 그 방 구독 연결을 끊습니다. 다시 연결하면 구독 검사에서 거부됩니다. 나가기 커밋 후 호출합니다. */
  public void closeRoom(Long userId, Long roomId) {
    sessions.forEach(
        (sessionId, session) -> {
          if (userId.equals(session.userId) && session.roomIds.contains(roomId)) {
            close(sessionId, LEFT_ROOM);
          }
        });
  }

  /** 로그아웃·탈퇴 등으로 폐기된 인증 세션의 연결을 끊습니다. 다시 연결해도 CONNECT 검사에서 거부됩니다. */
  public void closeAuthSessions(Collection<String> authSessionIds) {
    sessions.forEach(
        (sessionId, session) -> {
          if (authSessionIds.contains(session.authSessionId)) {
            close(sessionId, SESSION_REVOKED);
          }
        });
  }

  void close(String sessionId) {
    close(sessionId, JWT_EXPIRED);
  }

  private void close(String sessionId, CloseStatus status) {
    Session session = sessions.get(sessionId);
    if (session == null) return;
    try {
      session.socket.close(status);
    } catch (IOException e) {
      log.warn("Failed to close WebSocket session {} with {}", sessionId, status, e);
    } finally {
      remove(sessionId);
    }
  }

  void remove(String sessionId) {
    Session session = sessions.remove(sessionId);
    if (session != null) {
      synchronized (session) {
        if (session.expiration != null) session.expiration.cancel(false);
      }
    }
  }

  @PreDestroy
  void shutdown() {
    scheduler.shutdownNow();
    sessions.clear();
  }

  private static class Session {
    private final WebSocketSession socket;
    private final Set<Long> roomIds = ConcurrentHashMap.newKeySet();
    private volatile Long userId;
    private volatile String authSessionId;
    private ScheduledFuture<?> expiration;

    private Session(WebSocketSession socket) {
      this.socket = socket;
    }
  }
}
