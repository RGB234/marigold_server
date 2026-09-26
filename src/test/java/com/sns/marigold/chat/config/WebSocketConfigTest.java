package com.sns.marigold.chat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.csrf.CsrfTokenService;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.service.JwtAuthenticationService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.global.config.UrlProperties;
import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.tsid.TsidCodec;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

  @Mock private JwtAuthenticationService jwtAuthenticationService;

  @Mock private RoomParticipantRepository participantRepository;

  @Mock private CookieManager cookieManager;

  @Mock private AuditLogger auditLogger;
  @Mock private WebSocketTokenSessions tokenSessions;
  @Mock private StompProtocolErrorHandler stompProtocolErrorHandler;

  private WebSocketConfig webSocketConfig;

  @Test
  void connectRequiresJwt() {
    assertThatThrownBy(() -> inbound(connectAccessor("csrf-token")))
        .isInstanceOfSatisfying(
            AuthException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED));
    verifyNoInteractions(jwtAuthenticationService);
  }

  @Test
  void connectRejectsInvalidJwt() {
    StompHeaderAccessor accessor = connectAccessor("csrf-token");
    accessor.addNativeHeader("Authorization", "Bearer invalid");
    given(jwtAuthenticationService.getAuthentication("invalid"))
        .willThrow(new IllegalArgumentException("invalid"));
    assertThatThrownBy(() -> inbound(accessor))
        .isInstanceOfSatisfying(
            AuthException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID));
  }

  @Test
  void connectStoresVerifiedExpiryAndSchedulesClosure() {
    Instant expiresAt = Instant.now().plusSeconds(60);
    UsernamePasswordAuthenticationToken auth =
        (UsernamePasswordAuthenticationToken) authentication(1L);
    auth.setDetails(expiresAt);
    given(jwtAuthenticationService.getAuthentication("valid")).willReturn(auth);
    StompHeaderAccessor accessor = connectAccessor("csrf-token");
    accessor.setSessionId("session");
    accessor.addNativeHeader("Authorization", "Bearer valid");
    inbound(accessor);
    assertThat(accessor.getUser()).isSameAs(auth);
    assertThat(accessor.getSessionAttributes()).containsEntry("jwtExpiresAt", expiresAt);
    verify(tokenSessions).expireAt("session", expiresAt);
  }

  @Test
  void authenticatedSendDoesNotRevalidateJwt() {
    inbound(authenticatedMessage(StompCommand.SEND, "/pub/chat/message"));
    verifyNoInteractions(jwtAuthenticationService);
  }

  @Test
  void connectRejectsExpiredAuthentication() {
    UsernamePasswordAuthenticationToken auth =
        (UsernamePasswordAuthenticationToken) authentication(1L);
    auth.setDetails(Instant.now().minusSeconds(1));
    given(jwtAuthenticationService.getAuthentication("expired")).willReturn(auth);
    StompHeaderAccessor accessor = connectAccessor("csrf-token");
    accessor.addNativeHeader("Authorization", "Bearer expired");
    assertThatThrownBy(() -> inbound(accessor))
        .isInstanceOfSatisfying(
            AuthException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED));
    verifyNoInteractions(tokenSessions);
  }

  @Test
  void authenticatedSendRequiresConnectExpiry() {
    StompHeaderAccessor accessor = authenticatedMessage(StompCommand.SEND, "/pub/chat/message");
    accessor.getSessionAttributes().remove("jwtExpiresAt");
    assertThatThrownBy(() -> inbound(accessor))
        .isInstanceOfSatisfying(
            AuthException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/sub/chat/room/100", "/pub/other", "/pub/chat/message/", ""})
  void sendRejectsOtherDestinations(String destination) {
    assertThatThrownBy(() -> inbound(authenticatedMessage(StompCommand.SEND, destination)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/sub/other",
        "/pub/chat/message",
        "/sub/chat/room/*",
        "/sub/chat/room/100/extra",
        ""
      })
  void subscribeRejectsOtherDestinations(String destination) {
    assertThatThrownBy(() -> inbound(authenticatedMessage(StompCommand.SUBSCRIBE, destination)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void sendRejectsMissingUserEvenWithJwtHeader() {
    StompHeaderAccessor accessor = authenticatedMessage(StompCommand.SEND, "/pub/chat/message");
    accessor.setUser(null);
    accessor.addNativeHeader("Authorization", "Bearer valid");
    assertThatThrownBy(() -> inbound(accessor))
        .isInstanceOfSatisfying(
            AuthException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED));
    verifyNoInteractions(jwtAuthenticationService);
  }

  @Test
  void expiredMessageClosesConnection() {
    StompHeaderAccessor accessor = authenticatedMessage(StompCommand.SEND, "/pub/chat/message");
    accessor.getSessionAttributes().put("jwtExpiresAt", Instant.now().minusSeconds(1));
    assertThatThrownBy(() -> inbound(accessor))
        .isInstanceOfSatisfying(
            AuthException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED));
    verify(tokenSessions).close("session");
  }

  private StompHeaderAccessor authenticatedMessage(StompCommand command, String destination) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setSessionId("session");
    accessor.setUser(authentication(1L));
    accessor.setDestination(destination);
    accessor.setSessionAttributes(
        new HashMap<>(
            Map.of("csrfValidated", true, "jwtExpiresAt", Instant.now().plusSeconds(60))));
    return accessor;
  }

  private void inbound(StompHeaderAccessor accessor) {
    var registration =
        new ChannelRegistration() {
          public void receive(Message<?> message) {
            getInterceptors().get(0).preSend(message, mock(MessageChannel.class));
          }
        };
    webSocketConfig.configureClientInboundChannel(registration);
    accessor.setLeaveMutable(true);
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    registration.receive(message);
  }

  @BeforeEach
  void setUp() {
    webSocketConfig =
        new WebSocketConfig(
            jwtAuthenticationService,
            participantRepository,
            cookieManager,
            auditLogger,
            urlProperties(),
            tokenSessions,
            stompProtocolErrorHandler);
  }

  @Test
  @DisplayName("WebSocket CONNECT는 CSRF 쿠키와 헤더가 일치하면 통과한다.")
  void validateCsrf_AllowsMatchingToken() {
    // given
    StompHeaderAccessor accessor = connectAccessor("csrf-token");

    // when & then
    assertThatCode(() -> webSocketConfig.validateCsrf(accessor)).doesNotThrowAnyException();
    assertThat(accessor.getSessionAttributes()).containsEntry("csrfValidated", Boolean.TRUE);
  }

  @Test
  @DisplayName("WebSocket CONNECT는 CSRF 쿠키와 헤더가 불일치하면 거부한다.")
  void validateCsrf_DeniesMismatchedToken() {
    // given
    StompHeaderAccessor accessor = connectAccessor("other-token");

    // when & then
    assertThatThrownBy(() -> webSocketConfig.validateCsrf(accessor))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("WebSocket SEND/SUBSCRIBE는 CONNECT CSRF 검증이 선행되어야 한다.")
  void requireCsrfValidated_DeniesWhenConnectWasNotValidated() {
    // given
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setSessionAttributes(new HashMap<>());

    // when & then
    assertThatThrownBy(() -> webSocketConfig.requireCsrfValidated(accessor))
        .isInstanceOf(AccessDeniedException.class);
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

  @Test
  @DisplayName("인증된 사용자는 자신의 STOMP 오류 queue를 구독할 수 있다.")
  void authorizeSubscription_AllowsUserErrorQueue() {
    StompHeaderAccessor accessor =
        authenticatedMessage(StompCommand.SUBSCRIBE, "/user/queue/errors");

    assertThatCode(() -> inbound(accessor)).doesNotThrowAnyException();
    verifyNoInteractions(participantRepository);
  }

  @Test
  void resolveChatRoomSubscriptionIdRejectsDecimalFallback() {
    assertThatThrownBy(() -> webSocketConfig.resolveChatRoomSubscriptionId("/sub/chat/room/100"))
        .isInstanceOf(AccessDeniedException.class);
  }

  private StompHeaderAccessor connectAccessor(String csrfHeader) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, "csrf-token");
    accessor.setSessionAttributes(sessionAttributes);
    accessor.addNativeHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, csrfHeader);
    return accessor;
  }

  private StompHeaderAccessor chatRoomSubscribeAccessor(Long roomId, Long userId) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/sub/chat/room/" + TsidCodec.encode(roomId));
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

  private UrlProperties urlProperties() {
    return new UrlProperties(new UrlProperties.Frontend("http://localhost:8000", null), null);
  }
}
