package com.sns.marigold.chat.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.exception.ApplicationException;

public class ChatException extends ApplicationException {
  protected ChatException(@NonNull ChatError error) {
    super(error);
  }

  public static ChatException forEmptyMessage() {
    return new ChatException(ChatError.MESSAGE_EMPTY);
  }

  public static ChatException forRoomNotFound() {
    return new ChatException(ChatError.ROOM_NOT_FOUND);
  }

  public static ChatException forClosedRoom() {
    return new ChatException(ChatError.ROOM_CLOSED);
  }

  public static ChatException forInvalidRoomType() {
    return new ChatException(ChatError.ROOM_TYPE_INVALID);
  }
}
