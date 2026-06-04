package com.sns.marigold.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "파일 업로드 결과")
@Getter
@Builder
@AllArgsConstructor
public class FileUploadDto {
  @Schema(description = "스토리지에 저장된 파일명", example = "files/01JABCDEF1234.pdf")
  private String storedFileName;

  @Schema(description = "사용자가 업로드한 원본 파일명", example = "document.pdf")
  private String originalFileName;

  @Schema(description = "파일 MIME 타입", example = "application/pdf")
  private String contentType;

  @Schema(description = "파일 크기(byte)", example = "102400")
  private long fileSize;
}
