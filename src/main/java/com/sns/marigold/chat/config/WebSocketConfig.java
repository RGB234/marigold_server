package com.sns.marigold.chat.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.csrf.CsrfTokenService;
import com.sns.marigold.auth.common.service.JwtAuthenticationService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.chat.ChatDestinations;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.global.config.UrlProperties;

import io.hypersistence.tsid.TSID;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private static final String CSRF_VALIDATED_SESSION_ATTRIBUTE = "csrfValidated";
  private static final String JWT_EXPIRES_AT = "jwtExpiresAt";

  private final JwtAuthenticationService jwtAuthenticationService;
  private final RoomParticipantRepository participantRepository;
  private final CookieManager cookieManager;
  private final AuditLogger auditLogger;
  private final UrlProperties urlProperties;
  private final WebSocketTokenSessions tokenSessions;

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
    // SockJS 사용
    registry
        .addEndpoint("/ws")
        .setAllowedOrigins(allowedFrontendOrigin())
        .addInterceptors(csrfHandshakeInterceptor())
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
    config.enableSimpleBroker(ChatDestinations.BROKER_PREFIX); // 해당 접두어로 시작하는 경로를 구독
    config.setApplicationDestinationPrefixes(ChatDestinations.APPLICATION_PREFIX); // 클라이언트 전송 접두어
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
                  validateCsrf(accessor);
                  authenticate(accessor);
                }

                if (StompCommand.SEND.equals(accessor.getCommand())
                    || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                  requireCsrfValidated(accessor);
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

  private HandshakeInterceptor csrfHandshakeInterceptor() {
    return new HandshakeInterceptor() {
      @Override
      public boolean beforeHandshake(
          @NonNull ServerHttpRequest request,
          @NonNull ServerHttpResponse response,
          @NonNull WebSocketHandler wsHandler,
          @NonNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
          Cookie csrfCookie =
              cookieManager.getCookie(
                  servletRequest.getServletRequest(), CsrfTokenService.CSRF_TOKEN_COOKIE_NAME);
          if (csrfCookie != null) {
            attributes.put(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, csrfCookie.getValue());
          }
        }
        return true;
      }

      @Override
      public void afterHandshake(
          @NonNull ServerHttpRequest request,
          @NonNull ServerHttpResponse response,
          @NonNull WebSocketHandler wsHandler,
          @Nullable Exception exception) {}
    };
  }

  void validateCsrf(@NonNull StompHeaderAccessor accessor) {
    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    String csrfCookie = getCsrfCookieValue(sessionAttributes);
    String csrfHeader = accessor.getFirstNativeHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME);

    if (!constantTimeEquals(csrfCookie, csrfHeader)) {
      throw new AccessDeniedException("CSRF token is missing or invalid");
    }

    Objects.requireNonNull(sessionAttributes).put(CSRF_VALIDATED_SESSION_ATTRIBUTE, Boolean.TRUE);
  }

  void requireCsrfValidated(@NonNull StompHeaderAccessor accessor) {
    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
    if (sessionAttributes == null
        || !Boolean.TRUE.equals(sessionAttributes.get(CSRF_VALIDATED_SESSION_ATTRIBUTE))) {
      throw new AccessDeniedException("CSRF validation is required");
    }
  }

  @Nullable
  private String getCsrfCookieValue(@Nullable Map<String, Object> sessionAttributes) {
    if (sessionAttributes == null) {
      return null;
    }
    Object csrfCookie = sessionAttributes.get(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME);
    return csrfCookie instanceof String value ? value : null;
  }

  private boolean constantTimeEquals(@Nullable String left, @Nullable String right) {
    if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
      return false;
    }
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }

  void authenticate(@NonNull StompHeaderAccessor accessor) {
    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      throw new AccessDeniedException("JWT is required for CONNECT");
    }

    String token = authorizationHeader.substring(7);
    try {
      Authentication authentication = jwtAuthenticationService.getAuthentication(token);
      if (!authentication.isAuthenticated()
          || !(authentication.getDetails() instanceof Instant expiresAt)
          || !expiresAt.isAfter(Instant.now())) {
        throw new AccessDeniedException("JWT is missing an unexpired expiration");
      }
      Map<String, Object> attributes = Objects.requireNonNull(accessor.getSessionAttributes());
      tokenSessions.expireAt(Objects.requireNonNull(accessor.getSessionId()), expiresAt);
      attributes.put(JWT_EXPIRES_AT, expiresAt);
      accessor.setUser(authentication);
    } catch (Exception e) {
      auditLogger.warn("event=ws_invalid_token");
      throw new AccessDeniedException("JWT authentication failed", e);
    }
  }

  void requireAuthenticatedSession(StompHeaderAccessor accessor) {
    getAuthenticatedUserId(accessor.getUser());
    Map<String, Object> attributes = accessor.getSessionAttributes();
    if (attributes == null || !(attributes.get(JWT_EXPIRES_AT) instanceof Instant expiresAt)) {
      throw new AccessDeniedException("Authenticated CONNECT is required");
    }
    if (!expiresAt.isAfter(Instant.now())) {
      tokenSessions.close(Objects.requireNonNull(accessor.getSessionId()));
      throw new AccessDeniedException("JWT expired");
    }
  }

  void authorizeSubscription(@NonNull StompHeaderAccessor accessor) {
    Long roomId = resolveChatRoomSubscriptionId(accessor.getDestination());
    if (roomId == null) {
      throw new AccessDeniedException("SUBSCRIBE destination is not allowed");
    }

    Long userId = getAuthenticatedUserId(accessor.getUser());
    if (!participantRepository.existsByChatRoom_IdAndUser_Id(roomId, userId)) {
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
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof CustomPrincipal customPrincipal
        && customPrincipal.getUserId() != null) {
      return customPrincipal.getUserId();
    }
    throw new AccessDeniedException("Authentication is required for chat room subscription");
  }
}
