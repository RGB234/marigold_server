package com.sns.marigold.chat.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;

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
      sessions.expireAt("idle", Instant.now().plusMillis(100));
      verify(socket, timeout(2000)).close(new CloseStatus(4001, "JWT expired"));
    } finally {
      sessions.shutdown();
    }
  }

  @Test
  void disconnectedSessionCancelsExpiry() throws Exception {
    WebSocketTokenSessions sessions = new WebSocketTokenSessions();
    try {
      WebSocketSession socket = mock(WebSocketSession.class);
      when(socket.getId()).thenReturn("closed");
      sessions.register(socket);
      sessions.expireAt("closed", Instant.now().plusMillis(100));
      sessions.remove("closed");
      verify(socket, after(250).never()).close(any());
      assertThatThrownBy(() -> sessions.expireAt("closed", Instant.now()))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      sessions.shutdown();
    }
  }
}
