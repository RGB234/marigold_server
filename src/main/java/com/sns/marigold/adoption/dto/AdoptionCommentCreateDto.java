package com.sns.marigold.adoption.dto;

import java.util.List;

import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.storage.dto.UploadToken;

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
@Builder
@AllArgsConstructor
@Schema(description = "입양 댓글 생성 요청")
public class AdoptionCommentCreateDto {

  @Schema(description = "부모 댓글 ID. 대댓글이 아니면 null", example = "1", nullable = true)
  private Long parentId;

  @Schema(description = "댓글 내용. 최대 1000자", example = "입양을 희망합니다.")
  @NotBlank(message = "내용이 비어있습니다.")
  @Size(max = ValidationPolicy.Comment.CONTENT_MAX_LENGTH, message = "댓글은 1000자 이하여야 합니다.")
  private String content;

  @Schema(description = "댓글 이미지의 업로드 토큰 목록. 최대 1개")
  private List<UploadToken> uploadTokens;
}
