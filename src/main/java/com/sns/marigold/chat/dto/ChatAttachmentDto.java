package com.sns.marigold.chat.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "채팅 메시지 첨부파일")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachmentDto {

  @Schema(description = "TSID 형식 첨부파일 ID", type = "string", example = "01JABCDEF1234")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long id;

  @Schema(description = "원본 파일명", example = "photo.jpg")
  private String originalFileName;

  @Schema(description = "파일 MIME 타입", example = "image/jpeg")
  private String contentType;

  @Schema(description = "파일 크기(byte)", example = "204800")
  private long fileSize;

  @Schema(
      description = "다운로드 URL",
      example = "https://example.com/download/photo.jpg",
      nullable = true)
  private String downloadUrl;
}
