package com.sns.marigold.chat.controller;

import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.service.ChatService;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatService chatService;

  @PreAuthorize("isAuthenticated()")
  @MessageMapping("/chat/message") // 웹소켓
  public void message(ChatMessageDto messageDto) {
    log.info("Received message: {}", messageDto);
    ChatMessageDto savedMessage = chatService.saveMessage(messageDto);
    String roomIdStr = TSID.from(savedMessage.getRoomId()).toString();
    messagingTemplate.convertAndSend("/sub/chat/room/" + roomIdStr, savedMessage);
  }
}
