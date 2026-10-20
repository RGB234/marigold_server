package com.sns.marigold.chat.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sns.marigold.chat.exception.ChatError;
import com.sns.marigold.chat.exception.ChatException;

class ChatRoomTypeTest {

  @Test
  void blankValueUsesAll() {
    assertThat(ChatRoomType.fromString(" ")).isEqualTo(ChatRoomType.ALL);
  }

  @Test
  void unsupportedValueIsRejected() {
    assertThatThrownBy(() -> ChatRoomType.fromString("unexpected"))
        .isInstanceOf(ChatException.class)
        .satisfies(
            exception ->
                assertThat(((ChatException) exception).getErrorSpec())
                    .isEqualTo(ChatError.ROOM_TYPE_INVALID));
  }
}
