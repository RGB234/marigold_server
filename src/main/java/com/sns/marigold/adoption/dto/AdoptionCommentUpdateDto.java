package com.sns.marigold.adoption.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.global.validation.ValidationPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "입양 댓글 수정 요청")
public class AdoptionCommentUpdateDto {

  @Schema(description = "댓글 내용. 최대 1000자", example = "내용을 수정합니다.")
  @NotBlank(message = "내용이 비어있습니다.")
  @Size(max = ValidationPolicy.Comment.CONTENT_MAX_LENGTH, message = "댓글은 1000자 이하여야 합니다.")
  private String content;

  @Schema(description = "기존 댓글 이미지를 삭제할지 여부", defaultValue = "false", example = "false")
  private Boolean removeImage;

  @Schema(description = "새로 업로드할 댓글 이미지 파일 목록. 최대 1개", type = "string", format = "binary")
  private List<MultipartFile> images;

  public boolean shouldRemoveImage() {
    return Boolean.TRUE.equals(removeImage);
  }
}
