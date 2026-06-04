package com.sns.marigold.chat.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.global.util.TsidJacksonConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "채팅방 정보")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

  @Schema(description = "TSID 형식 채팅방 ID", type = "string", example = "01JABCDEF1234")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long id;

  @Schema(description = "입양 게시글 ID", example = "1")
  private Long postId;

  @Schema(description = "입양 게시글 제목", example = "가족을 찾습니다")
  private String postTitle;

  @Schema(description = "TSID 형식 게시글 작성자 ID", type = "string", example = "01JABCDEG5678")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long postWriterId;

  @Schema(description = "채팅방 생성 시각", example = "2026-06-04T12:34:56")
  private LocalDateTime createdAt;

  @Schema(description = "TSID 형식 참여자 1 ID", type = "string", example = "01JABCDEH9012")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long user1Id;

  @Schema(description = "참여자 1 닉네임", example = "작성자")
  private String user1Nickname;

  @Schema(description = "TSID 형식 참여자 2 ID", type = "string", example = "01JABCDEK3456")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long user2Id;

  @Schema(description = "참여자 2 닉네임", example = "입양희망자")
  private String user2Nickname;

  @Schema(description = "채팅방 상태", example = "ACTIVE")
  private ChatRoomStatus status;
}
