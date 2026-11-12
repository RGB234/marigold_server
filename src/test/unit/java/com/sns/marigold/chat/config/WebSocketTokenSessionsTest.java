package com.sns.marigold.chat.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class WebSocketTokenSessionsTest {
  @Test
  void closesIdleConnectionAtExpiry() throws Exception {
    WebSocketTokenSessions sessions = new WebSocketTokenSessions();
    try {
      WebSocketSession socket = mock(WebSocketSession.class);
      when(socket.getId()).thenReturn("idle");
      sessions.register(socket);
      sessions.expireAt("idle", "auth-1", Instant.now().plusMillis(100));
      verify(socket, timeout(2000)).close(new CloseStatus(4001, "JWT expired"));
    } finally {
      sessions.shutdown();
    }
  }

  @Test
  void leavingRoomClosesOnlyThatUsersSubscriptionsToTheRoom() throws Exception {
    WebSocketTokenSessions sessions = new WebSocketTokenSessions();
    try {
      WebSocketSession left = register(sessions, "left");
      WebSocketSession otherRoom = register(sessions, "other-room");
      WebSocketSession otherUser = register(sessions, "other-user");
      sessions.subscribe("left", 1L, 100L);
      sessions.subscribe("other-room", 1L, 200L);
      sessions.subscribe("other-user", 2L, 100L);

      sessions.closeRoom(1L, 100L);

      verify(left).close(new CloseStatus(4003, "Left chat room"));
      verify(otherRoom, never()).close(any());
      verify(otherUser, never()).close(any());
    } finally {
      sessions.shutdown();
    }
  }

  @Test
  void revokedAuthSessionClosesOnlyItsConnections() throws Exception {
    WebSocketTokenSessions sessions = new WebSocketTokenSessions();
    try {
      WebSocketSession revoked = register(sessions, "revoked");
      WebSocketSession other = register(sessions, "other");
      sessions.expireAt("revoked", "auth-1", Instant.now().plusSeconds(60));
      sessions.expireAt("other", "auth-2", Instant.now().plusSeconds(60));

      sessions.closeAuthSessions(List.of("auth-1"));

      verify(revoked).close(new CloseStatus(4002, "Session revoked"));
      verify(other, never()).close(any());
    } finally {
      sessions.shutdown();
    }
  }

  private WebSocketSession register(WebSocketTokenSessions sessions, String id) {
    WebSocketSession socket = mock(WebSocketSession.class);
    when(socket.getId()).thenReturn(id);
    sessions.register(socket);
    return socket;
  }

  @Test
  void disconnectedSessionCancelsExpiry() throws Exception {
    WebSocketTokenSessions sessions = new WebSocketTokenSessions();
    try {
      WebSocketSession socket = mock(WebSocketSession.class);
      when(socket.getId()).thenReturn("closed");
      sessions.register(socket);
      sessions.expireAt("closed", "auth-1", Instant.now().plusMillis(100));
      sessions.remove("closed");
      verify(socket, after(250).never()).close(any());
      assertThatThrownBy(() -> sessions.expireAt("closed", "auth-1", Instant.now()))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      sessions.shutdown();
    }
  }
}
