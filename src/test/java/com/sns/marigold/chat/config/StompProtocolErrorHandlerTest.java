package com.sns.marigold.chat.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.exception.AuthException;

class StompProtocolErrorHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private StompProtocolErrorHandler errorHandler;

  @BeforeEach
  void setUp() {
    errorHandler = new StompProtocolErrorHandler(objectMapper);
  }

  @Test
  void convertsNestedBusinessExceptionToJsonErrorFrame() throws Exception {
    Message<byte[]> clientMessage = message(StompCommand.CONNECT, null);
    MessageDeliveryException exception =
        new MessageDeliveryException(clientMessage, AuthException.forExpiredToken());

    Message<byte[]> result =
        errorHandler.handleClientMessageProcessingError(clientMessage, exception);

    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
    JsonNode payload = objectMapper.readTree(result.getPayload());
    assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(accessor.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(accessor.getFirstNativeHeader(StompProtocolErrorHandler.ERROR_CODE_HEADER))
        .isEqualTo("AUTH_TOKEN_EXPIRED");
    assertThat(payload.get("errorCode").asText()).isEqualTo("AUTH_TOKEN_EXPIRED");
    assertThat(payload.get("fatal").asBoolean()).isTrue();
    assertThat(payload.get("command").asText()).isEqualTo("CONNECT");
  }

  @Test
  void convertsAccessDeniedExceptionWithoutExposingInternalReason() throws Exception {
    Message<byte[]> clientMessage = message(StompCommand.SUBSCRIBE, "/sub/private");
    AccessDeniedException exception = new AccessDeniedException("sensitive internal reason");

    Message<byte[]> result =
        errorHandler.handleClientMessageProcessingError(clientMessage, exception);

    String payloadText = new String(result.getPayload(), StandardCharsets.UTF_8);
    JsonNode payload = objectMapper.readTree(payloadText);
    assertThat(payload.get("errorCode").asText()).isEqualTo("AUTH_ACCESS_DENIED");
    assertThat(payload.get("destination").asText()).isEqualTo("/sub/private");
    assertThat(payloadText).doesNotContain("sensitive internal reason");
  }

  private Message<byte[]> message(StompCommand command, String destination) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    if (destination != null) {
      accessor.setDestination(destination);
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
