package com.sns.marigold.chat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 목록 조회 타입. WRITER=내가 게시글 작성자인 채팅방, INQUIRER=내가 문의자인 채팅방, ALL=전체")
public enum ChatRoomType {
  @Schema(description = "내가 입양 게시글 작성자인 채팅방")
  WRITER,
  @Schema(description = "내가 입양 문의자인 채팅방")
  INQUIRER,
  @Schema(description = "전체 채팅방")
  ALL;

  @JsonCreator
  public static ChatRoomType fromString(String value) {
    if (value == null || value.isBlank()) {
      return ALL;
    }
    try {
      return ChatRoomType.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ALL;
    }
  }
}
