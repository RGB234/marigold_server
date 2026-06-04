package com.sns.marigold.chat.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Schema(description = "채팅 메시지")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatMessageDto {

  @Schema(description = "TSID 형식 채팅방 ID", type = "string", example = "01JABCDEF1234")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long roomId;

  @Schema(description = "TSID 형식 발신자 ID", type = "string", example = "01JABCDEG5678")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long senderId;

  @Schema(description = "발신자 닉네임", example = "마리골드")
  private String senderNickname;

  @Schema(
      description = "발신자 프로필 이미지 URL",
      example = "https://example.com/profile.jpg",
      nullable = true)
  private String senderImageUrl;

  @Schema(description = "메시지 본문", example = "안녕하세요.")
  private String message;

  @Schema(description = "메시지 타입", example = "TEXT")
  private String messageType;

  @Schema(description = "첨부파일 목록")
  @Builder.Default
  private List<ChatAttachmentDto> attachments = Collections.emptyList();

  @Schema(description = "생성 시각", example = "2026-06-04T12:34:56")
  private LocalDateTime createdAt;
}
