package com.sns.marigold.chat.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;

public class ChatException extends BusinessException {
  protected ChatException(@NonNull ErrorCode errorCode) {
    super(errorCode);
  }

  public static ChatException forEmptyMessage() {
    return new ChatException(ErrorCode.CHAT_MESSAGE_EMPTY);
  }
}
