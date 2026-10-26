package com.sns.marigold.chat.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.chat.config.WebSocketTokenSessions;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatMessageRequestDto;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.global.tsid.TsidCodec;
import com.sns.marigold.global.tsid.TsidValue;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Mock private ChatService chatService;

  @Mock private WebSocketTokenSessions tokenSessions;

  @InjectMocks private ChatWebSocketController chatController;

  @InjectMocks private ChatController restController;

  @Test
  @DisplayName("WebSocket 메시지 발신자는 Principal의 인증 사용자다.")
  void message_UsesAuthenticatedUserAsSender() {
    // given
    ChatMessageRequestDto request =
        ChatMessageRequestDto.builder().roomId(100L).message("hello").build();
    ChatMessageDto savedMessage =
        ChatMessageDto.builder().roomId(100L).senderId(1L).message("hello").build();

    given(chatService.saveMessage(same(request), eq(1L))).willReturn(savedMessage);

    CustomPrincipal principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority("ROLE_PERSON")),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());

    // when
    chatController.message(request, authentication);

    // then
    verify(chatService).saveMessage(same(request), eq(1L));
    verify(messagingTemplate)
        .convertAndSend(eq("/sub/chat/room/" + TsidCodec.encode(100L)), same(savedMessage));
  }

  @Test
  @DisplayName("방 나가기가 커밋된 뒤 그 방을 구독 중인 연결을 끊는다.")
  void leaveRoom_ClosesRoomSubscriptionsAfterLeaving() {
    CustomPrincipal principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority("ROLE_PERSON")),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);

    restController.leaveRoom(principal, new TsidValue(100L));

    InOrder inOrder = inOrder(chatService, tokenSessions);
    inOrder.verify(chatService).leaveRoom(100L, 1L);
    inOrder.verify(tokenSessions).closeRoom(1L, 100L);
  }
}
