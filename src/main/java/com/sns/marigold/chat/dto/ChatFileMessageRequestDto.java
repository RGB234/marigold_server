package com.sns.marigold.chat.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "채팅 파일 메시지 생성 요청")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFileMessageRequestDto {

  @Schema(description = "메시지 본문", nullable = true, example = "사진 보내드려요.")
  private String message;

  @Schema(description = "첨부파일의 업로드 토큰 목록")
  private List<String> uploadTokens;
}
