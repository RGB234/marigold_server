package com.sns.marigold.chat.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "채팅방 생성 또는 조회 요청")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewChatDto {
  @Schema(description = "입양 게시글 ID", example = "1")
  @NotNull
  private Long adoptionPostId;

  @Schema(description = "TSID 형식 상대 사용자 ID", type = "string", example = "01JABCDEF1234")
  @NotNull
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long receiverId;
}
