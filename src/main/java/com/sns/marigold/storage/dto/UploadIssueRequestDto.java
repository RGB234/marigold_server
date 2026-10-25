package com.sns.marigold.storage.dto;

import java.util.List;

import com.sns.marigold.storage.service.UploadPurpose;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "업로드 URL 발급 요청")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadIssueRequestDto {
  @Schema(description = "업로드 용도", example = "ADOPTION_POST_IMAGE")
  @NotNull
  private UploadPurpose purpose;

  @Schema(description = "올릴 파일 목록")
  @NotEmpty
  private List<@NotNull @Valid FileItem> files;

  @Schema(description = "올릴 파일 정보")
  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FileItem {
    @Schema(description = "원본 파일명", example = "cat.jpg")
    @NotBlank
    @Size(max = 255)
    private String fileName;

    @Schema(description = "파일 크기(byte)", example = "102400")
    @Positive
    private long size;
  }
}
