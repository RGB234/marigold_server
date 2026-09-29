package com.sns.marigold.chat.exception;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.FailureKind;

public enum ChatError implements ErrorSpec {
  MESSAGE_EMPTY("CHAT_MESSAGE_EMPTY", "메시지 또는 첨부파일을 입력해주세요.", FailureKind.INVALID_INPUT),
  ROOM_NOT_FOUND("CHAT_ROOM_NOT_FOUND", "존재하지 않는 채팅방입니다.", FailureKind.NOT_FOUND),
  ROOM_CLOSED("CHAT_ROOM_CLOSED", "종료된 채팅방에는 메시지를 보낼 수 없습니다.", FailureKind.CONFLICT),
  ROOM_TYPE_INVALID("CHAT_ROOM_TYPE_INVALID", "채팅방 조회 유형이 올바르지 않습니다.", FailureKind.INVALID_INPUT);

  private final String code;
  private final String publicMessage;
  private final FailureKind kind;

  ChatError(String code, String publicMessage, FailureKind kind) {
    this.code = code;
    this.publicMessage = publicMessage;
    this.kind = kind;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String publicMessage() {
    return publicMessage;
  }

  @Override
  public FailureKind kind() {
    return kind;
  }
}
