package com.sns.marigold.chat.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 상태. ACTIVE=이용 가능, CLOSED=종료됨")
public enum ChatRoomStatus {
  @Schema(description = "이용 가능한 채팅방")
  ACTIVE,
  @Schema(description = "종료된 채팅방")
  CLOSED
}
