package com.sns.marigold.chat.config;

import com.sns.marigold.auth.common.jwt.JwtManager;
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

  private final JwtManager jwtManager;

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

            if (accessor != null
                && (StompCommand.CONNECT.equals(accessor.getCommand())
                    || (accessor.getUser() == null
                        && StompCommand.SEND.equals(accessor.getCommand())))) {
              authenticate(accessor);
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
      Authentication authentication = jwtManager.getAuthentication(token);
      accessor.setUser(authentication);
    } catch (Exception e) {
      log.error("WebSocket token authentication failed: {}", e.getMessage());
    }
  }
}
