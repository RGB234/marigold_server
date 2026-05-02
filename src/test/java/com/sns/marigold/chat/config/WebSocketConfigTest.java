package com.sns.marigold.chat.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.service.JwtAuthenticationService;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import io.hypersistence.tsid.TSID;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

  @Mock private JwtAuthenticationService jwtAuthenticationService;

  @Mock private RoomParticipantRepository participantRepository;

  private WebSocketConfig webSocketConfig;

  @BeforeEach
  void setUp() {
    webSocketConfig = new WebSocketConfig(jwtAuthenticationService, participantRepository);
  }

  @Test
  @DisplayName("채팅방 참여자는 해당 방을 구독할 수 있다.")
  void authorizeSubscription_AllowsParticipant() {
    // given
    StompHeaderAccessor accessor = chatRoomSubscribeAccessor(100L, 1L);
    given(participantRepository.existsByChatRoom_IdAndUser_Id(100L, 1L)).willReturn(true);

    // when & then
    assertThatCode(() -> webSocketConfig.authorizeSubscription(accessor))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("채팅방 참여자가 아니라면 해당 방을 구독할 수 없다.")
  void authorizeSubscription_DeniesNonParticipant() {
    // given
    StompHeaderAccessor accessor = chatRoomSubscribeAccessor(100L, 3L);
    given(participantRepository.existsByChatRoom_IdAndUser_Id(100L, 3L)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> webSocketConfig.authorizeSubscription(accessor))
        .isInstanceOf(AccessDeniedException.class);
  }

  private StompHeaderAccessor chatRoomSubscribeAccessor(Long roomId, Long userId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/sub/chat/room/" + TSID.from(roomId));
    accessor.setUser(authentication(userId));
    return accessor;
  }

  private Authentication authentication(Long userId) {
    CustomPrincipal principal =
        new CustomPrincipal(
            userId,
            List.of(new SimpleGrantedAuthority("ROLE_PERSON")),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
    return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
  }
}
