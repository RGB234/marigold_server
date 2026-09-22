package com.sns.marigold.chat.config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
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

/** 인증 토큰 만료 시 수신 전용 연결까지 종료하고 종료 예약을 정리합니다. */
@Component
@Slf4j
public class WebSocketTokenSessions {
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

  public WebSocketTokenSessions() {
    scheduler.setRemoveOnCancelPolicy(true);
  }

  void register(WebSocketSession socket) {
    sessions.put(socket.getId(), new Session(socket));
  }

  void expireAt(String sessionId, Instant expiresAt) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      throw new AccessDeniedException("WebSocket session is closed");
    }
    synchronized (session) {
      if (sessions.get(sessionId) != session || session.expiration != null) {
        throw new AccessDeniedException("Invalid CONNECT session");
      }
      long delay = Math.max(0, expiresAt.toEpochMilli() - System.currentTimeMillis());
      session.expiration = scheduler.schedule(() -> close(sessionId), delay, TimeUnit.MILLISECONDS);
    }
  }

  void close(String sessionId) {
    Session session = sessions.get(sessionId);
    if (session == null) return;
    try {
      session.socket.close(new CloseStatus(4001, "JWT expired"));
    } catch (IOException e) {
      log.warn("Failed to close expired WebSocket session {}", sessionId, e);
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
    private ScheduledFuture<?> expiration;

    private Session(WebSocketSession socket) {
      this.socket = socket;
    }
  }
}
