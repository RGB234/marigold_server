package com.sns.marigold.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "이미지 업로드 결과")
@Getter
@Builder
@AllArgsConstructor
public class ImageUploadDto {
  @Schema(description = "스토리지에 저장된 파일명", example = "images/01JABCDEF1234.jpg")
  private String storedFileName;

  @Schema(description = "사용자가 업로드한 원본 파일명", example = "cat.jpg")
  private String originalFileName;
}
