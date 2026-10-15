package com.sns.marigold.chat.dto;

import com.sns.marigold.global.tsid.TsidId;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "채팅 메시지 전송 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDto {

  @Schema(description = "TSID 형식 채팅방 ID", type = "string", example = "01JABCDEF1234")
  @TsidId
  @NotNull(message = "채팅방 ID는 필수입니다.")
  private Long roomId;

  @Schema(description = "메시지 본문", example = "안녕하세요.")
  @NotBlank(message = "메시지를 입력해주세요.")
  private String message;
}
