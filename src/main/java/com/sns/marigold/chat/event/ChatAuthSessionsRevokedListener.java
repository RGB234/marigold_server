package com.sns.marigold.chat.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sns.marigold.auth.event.AuthSessionsRevokedEvent;
import com.sns.marigold.chat.config.WebSocketTokenSessions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatAuthSessionsRevokedListener {
  private final WebSocketTokenSessions tokenSessions;

  // 이 인스턴스에 붙은 연결만 끊는다. 다른 서버의 연결은 access token 만료로 끊긴다
  @EventListener
  public void handle(AuthSessionsRevokedEvent event) {
    tokenSessions.closeAuthSessions(event.sessionIds());
  }
}
