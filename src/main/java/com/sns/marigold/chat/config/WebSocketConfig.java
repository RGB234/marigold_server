package com.sns.marigold.chat.config;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.service.JwtAuthenticationService;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import io.hypersistence.tsid.TSID;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private static final String CHAT_ROOM_SUBSCRIPTION_PREFIX = "/sub/chat/room/";

  private final JwtAuthenticationService jwtAuthenticationService;
  private final RoomParticipantRepository participantRepository;

  @Override
  public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
    // SockJS 사용
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
  }

  @Override
  public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
    config.enableSimpleBroker("/sub"); // 해당 접두어로 시작하는 경로를 구독
    config.setApplicationDestinationPrefixes("/pub"); // 클라이언트에서 서버로 메시지를 보낼 때 사용하는 접두어
  }

  @Override
  public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
    registration.interceptors(
        new ExecutorChannelInterceptor() {

          @Override
          @Nullable
          public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
            StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null) {
              if (StompCommand.CONNECT.equals(accessor.getCommand())
                  || (accessor.getUser() == null
                      && (StompCommand.SEND.equals(accessor.getCommand())
                          || StompCommand.SUBSCRIBE.equals(accessor.getCommand())))) {
                authenticate(accessor);
              }

              if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                authorizeSubscription(accessor);
              }
            }
            return message;
          }

          @Override
          @Nullable
          public Message<?> beforeHandle(
              @NonNull Message<?> message,
              @NonNull MessageChannel channel,
              @NonNull MessageHandler handler) {
            StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && accessor.getUser() instanceof Authentication authentication) {
              SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            return message;
          }

          @Override
          public void afterMessageHandled(
              @NonNull Message<?> message,
              @NonNull MessageChannel channel,
              @NonNull MessageHandler handler,
              @Nullable Exception ex) {
            SecurityContextHolder.clearContext();
          }
        });
  }

  private void authenticate(@NonNull StompHeaderAccessor accessor) {
    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      return;
    }

    String token = authorizationHeader.substring(7);
    try {
      Authentication authentication = jwtAuthenticationService.getAuthentication(token);
      accessor.setUser(authentication);
    } catch (Exception e) {
      log.error("WebSocket token authentication failed: {}", e.getMessage());
    }
  }

  void authorizeSubscription(@NonNull StompHeaderAccessor accessor) {
    Long roomId = resolveChatRoomSubscriptionId(accessor.getDestination());
    if (roomId == null) {
      return;
    }

    Long userId = getAuthenticatedUserId(accessor.getUser());
    if (!participantRepository.existsByChatRoom_IdAndUser_Id(roomId, userId)) {
      throw new AccessDeniedException("User is not a participant of chat room: " + roomId);
    }
  }

  @Nullable
  Long resolveChatRoomSubscriptionId(@Nullable String destination) {
    if (!StringUtils.hasText(destination)
        || !destination.startsWith(CHAT_ROOM_SUBSCRIPTION_PREFIX)) {
      return null;
    }

    String roomId = destination.substring(CHAT_ROOM_SUBSCRIPTION_PREFIX.length());
    try {
      return TSID.from(roomId).toLong();
    } catch (Exception e) {
      try {
        return Long.parseLong(roomId);
      } catch (NumberFormatException nfe) {
        throw new AccessDeniedException("Invalid chat room destination: " + destination);
      }
    }
  }

  private Long getAuthenticatedUserId(@Nullable Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof CustomPrincipal customPrincipal
        && customPrincipal.getUserId() != null) {
      return customPrincipal.getUserId();
    }
    throw new AccessDeniedException("Authentication is required for chat room subscription");
  }
}
