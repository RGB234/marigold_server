package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "입양 게시글과 연결된 채팅방 정보")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionPostWithChatDto {

  @Schema(description = "입양 게시글 요약")
  private AdoptionPostDto adoptionPost;

  @Schema(description = "TSID 형식 채팅방 ID", type = "string", example = "01JABCDEF1234")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long chatRoomId;

  @Schema(description = "TSID 형식 상대 사용자 ID", type = "string", example = "01JABCDEG5678")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long receiverId;

  @Schema(description = "상대 사용자 닉네임", example = "입양희망자")
  private String receiverNickname;

  @Schema(description = "채팅방 생성 시각", example = "2026-06-04T12:34:56")
  private LocalDateTime chatCreatedAt;
}
