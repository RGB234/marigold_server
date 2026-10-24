package com.sns.marigold.chat.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.user.event.UserDeletedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatUserDeletedListener {
  private final ChatService chatService;

  @EventListener
  public void handle(UserDeletedEvent event) {
    chatService.closeAllChatRoomsByUserId(event.userId());
  }
}
