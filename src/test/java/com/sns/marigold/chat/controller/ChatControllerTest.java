package com.sns.marigold.chat.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.service.ChatService;
import io.hypersistence.tsid.TSID;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  @Mock private ChatService chatService;

  @InjectMocks private ChatController chatController;

  @Test
  @DisplayName("WebSocket 메시지 발신자는 클라이언트 senderId가 아니라 인증 사용자다.")
  void message_UsesAuthenticatedUserAsSender() {
    // given
    ChatMessageDto request =
        ChatMessageDto.builder().roomId(100L).senderId(2L).message("hello").build();
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
        .convertAndSend(eq("/sub/chat/room/" + TSID.from(100L)), same(savedMessage));
  }
}
