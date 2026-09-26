package com.sns.marigold.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.StompErrorResponse;
import com.sns.marigold.chat.exception.ChatException;

class StompExceptionHandlerTest {

  private StompExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new StompExceptionHandler(org.mockito.Mockito.mock(AuditLogger.class));
  }

  @Test
  void convertsBusinessExceptionToRecoverableError() {
    Message<byte[]> message = message();

    StompErrorResponse response =
        exceptionHandler.handleBusinessException(ChatException.forEmptyMessage(), message);

    assertThat(response.getErrorCode()).isEqualTo("CHAT_MESSAGE_EMPTY");
    assertThat(response.isFatal()).isFalse();
    assertThat(response.getCommand()).isEqualTo("SEND");
    assertThat(response.getDestination()).isEqualTo("/pub/chat/message");
  }

  @Test
  void includesFieldErrorsForInvalidPayload() throws Exception {
    Message<byte[]> message = message();
    ChatMessageDto target = new ChatMessageDto();
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "messageDto");
    bindingResult.addError(new FieldError("messageDto", "message", "메시지를 입력해주세요."));
    Method method =
        ChatWebSocketController.class.getMethod("message", ChatMessageDto.class, Principal.class);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(message, new MethodParameter(method, 0), bindingResult);

    StompErrorResponse response = exceptionHandler.handleValidationException(exception, message);

    assertThat(response.getErrorCode()).isEqualTo("INVALID_INPUT_VALUE");
    assertThat(response.getErrors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.getField()).isEqualTo("message");
              assertThat(error.getMessage()).isEqualTo("메시지를 입력해주세요.");
            });
  }

  private Message<byte[]> message() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination("/pub/chat/message");
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
