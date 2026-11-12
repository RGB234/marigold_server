package com.sns.marigold.chat.config;

import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.AccessTokenDetails;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.service.JwtAuthenticationService;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.ChatDestinations;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.global.config.UrlProperties;
import com.sns.marigold.global.error.exception.ApplicationException;
import com.sns.marigold.global.tsid.TsidCodec;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private static final String JWT_EXPIRES_AT = "jwtExpiresAt";

  private final JwtAuthenticationService jwtAuthenticationService;
  private final RoomParticipantRepository participantRepository;
  private final AuditLogger auditLogger;
  private final UrlProperties urlProperties;
  private final WebSocketTokenSessions tokenSessions;
  private final StompProtocolErrorHandler stompProtocolErrorHandler;

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(
        handler ->
            new WebSocketHandlerDecorator(handler) {
              @Override
              public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                tokenSessions.register(session);
                try {
                  super.afterConnectionEstablished(session);
                } catch (Exception e) {
                  tokenSessions.remove(session.getId());
                  throw e;
                }
              }

              @Override
              public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
                  throws Exception {
                tokenSessions.remove(session.getId());
                super.afterConnectionClosed(session, status);
              }
            });
  }

  @Override
  public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
    registry.setErrorHandler(stompProtocolErrorHandler);
    // SockJS 사용
    registry
        .addEndpoint("/ws")
        // CONNECT는 쿠키가 아닌 Authorization 헤더로 인증하므로 CSRF 검사가 필요 없고, Origin 검사로 다른 사이트의 연결을 막는다
        .setAllowedOrigins(allowedFrontendOrigin())
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
    config.enableSimpleBroker(ChatDestinations.BROKER_PREFIX, ChatDestinations.QUEUE_PREFIX);
    config.setApplicationDestinationPrefixes(ChatDestinations.APPLICATION_PREFIX); // 클라이언트 전송 접두어
    config.setUserDestinationPrefix(ChatDestinations.USER_DESTINATION_PREFIX);
  }

  @Override
  public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
    registration.interceptors(
        new ExecutorChannelInterceptor() {

          @Override
          public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
            StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null) {
              try {
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                  authenticate(accessor);
                }

                if (StompCommand.SEND.equals(accessor.getCommand())
                    || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                  requireAuthenticatedSession(accessor);
                  if (StompCommand.SEND.equals(accessor.getCommand())
                      && !ChatDestinations.MESSAGE_SEND.equals(accessor.getDestination())) {
                    throw new AccessDeniedException("SEND destination is not allowed");
                  }
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                  authorizeSubscription(accessor);
                }
              } catch (RuntimeException e) {
                auditLogger.warn(
                    "event=websocket_message_rejected command={} destination={} sessionId={}"
                        + " hasUser={} reason={}",
                    accessor.getCommand(),
                    accessor.getDestination(),
                    accessor.getSessionId(),
                    accessor.getUser() != null,
                    e.getMessage());
                throw e;
              }
            }
            return message;
          }

          @Override
          public Message<?> beforeHandle(
              @NonNull Message<?> message,
              @NonNull MessageChannel channel,
              @NonNull MessageHandler handler) {
            StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && accessor.getUser() instanceof Authentication authentication) {
              if (StompCommand.SEND.equals(accessor.getCommand())
                  || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                requireAuthenticatedSession(accessor);
              }
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

  private String allowedFrontendOrigin() {
    URI frontendUri = URI.create(urlProperties.frontend().origin());
    return frontendUri.getScheme() + "://" + frontendUri.getAuthority();
  }

  void authenticate(@NonNull StompHeaderAccessor accessor) {
    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      throw AuthException.forUnauthorized();
    }

    String token = authorizationHeader.substring(7);
    Authentication authentication;
    try {
      authentication = jwtAuthenticationService.getAuthentication(token);
    } catch (ApplicationException e) {
      auditLogger.warn("event=ws_authentication_rejected code={}", e.getErrorSpec().code());
      throw e;
    } catch (ExpiredJwtException e) {
      auditLogger.warn("event=ws_authentication_rejected code=AUTH_TOKEN_EXPIRED");
      throw AuthException.forExpiredToken(e);
    } catch (JwtException | IllegalArgumentException e) {
      auditLogger.warn("event=ws_authentication_rejected code=AUTH_TOKEN_INVALID");
      throw AuthException.forInvalidToken(e);
    } catch (RuntimeException e) {
      throw AuthException.forInternalServerError(e);
    }

    if (!authentication.isAuthenticated()
        || !(authentication.getDetails() instanceof AccessTokenDetails details)) {
      throw AuthException.forInvalidToken();
    }
    Instant expiresAt = details.expiresAt();
    if (!expiresAt.isAfter(Instant.now())) {
      throw AuthException.forExpiredToken();
    }

    Map<String, Object> attributes = Objects.requireNonNull(accessor.getSessionAttributes());
    tokenSessions.expireAt(
        Objects.requireNonNull(accessor.getSessionId()), details.sessionId(), expiresAt);
    attributes.put(JWT_EXPIRES_AT, expiresAt);
    accessor.setUser(authentication);
  }

  void requireAuthenticatedSession(StompHeaderAccessor accessor) {
    getAuthenticatedUserId(accessor.getUser());
    Map<String, Object> attributes = accessor.getSessionAttributes();
    if (attributes == null || !(attributes.get(JWT_EXPIRES_AT) instanceof Instant expiresAt)) {
      throw AuthException.forUnauthorized();
    }
    if (!expiresAt.isAfter(Instant.now())) {
      tokenSessions.close(Objects.requireNonNull(accessor.getSessionId()));
      throw AuthException.forExpiredToken();
    }
  }

  void authorizeSubscription(@NonNull StompHeaderAccessor accessor) {
    if (ChatDestinations.USER_ERROR_QUEUE.equals(accessor.getDestination())) {
      return;
    }

    Long roomId = resolveChatRoomSubscriptionId(accessor.getDestination());
    if (roomId == null) {
      throw new AccessDeniedException("SUBSCRIBE destination is not allowed");
    }

    Long userId = getAuthenticatedUserId(accessor.getUser());
    tokenSessions.subscribe(Objects.requireNonNull(accessor.getSessionId()), userId, roomId);
    if (!participantRepository.existsByChatRoom_IdAndUser_IdAndLeavedAtIsNull(roomId, userId)) {
      throw new AccessDeniedException("User is not a participant of chat room: " + roomId);
    }
  }

  @Nullable
  Long resolveChatRoomSubscriptionId(@Nullable String destination) {
    if (!StringUtils.hasText(destination)
        || !destination.startsWith(ChatDestinations.ROOM_SUBSCRIPTION_PREFIX)) {
      return null;
    }

    String roomId = destination.substring(ChatDestinations.ROOM_SUBSCRIPTION_PREFIX.length());
    try {
      return TsidCodec.decode(roomId);
    } catch (IllegalArgumentException e) {
      throw new AccessDeniedException("Invalid chat room destination: " + destination, e);
    }
  }

  private Long getAuthenticatedUserId(@Nullable Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof CustomPrincipal customPrincipal
        && customPrincipal.getUserId() != null) {
      return customPrincipal.getUserId();
    }
    throw AuthException.forUnauthorized();
  }
}
