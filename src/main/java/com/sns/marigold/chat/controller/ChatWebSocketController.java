package com.sns.marigold.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.ChatDestinations;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.service.ChatService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Chat WebSocket", description = "채팅 웹소켓 API")
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatService chatService;

  @PreAuthorize("isAuthenticated()")
  @MessageMapping(ChatDestinations.MESSAGE_MAPPING)
  public void message(ChatMessageDto messageDto, Principal principal) {
    try {
      ChatMessageDto savedMessage =
          chatService.saveMessage(messageDto, getAuthenticatedUserId(principal));
      messagingTemplate.convertAndSend(
          ChatDestinations.room(savedMessage.getRoomId()), savedMessage);
    } catch (RuntimeException e) {
      log.warn(
          "Failed to process WebSocket chat message. roomId={}, authenticated={}",
          messageDto == null ? null : messageDto.getRoomId(),
          principal != null,
          e);
      throw e;
    }
  }

  private Long getAuthenticatedUserId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof CustomPrincipal customPrincipal
        && customPrincipal.getUserId() != null) {
      return customPrincipal.getUserId();
    }
    throw AuthException.forUnauthorized();
  }
}
