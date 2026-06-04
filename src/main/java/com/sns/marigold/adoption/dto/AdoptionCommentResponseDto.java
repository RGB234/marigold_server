package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.sns.marigold.adoption.entity.AdoptionComment;
import com.sns.marigold.user.dto.response.UserInfoDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "입양 댓글 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdoptionCommentResponseDto {

  @Schema(description = "댓글 ID", example = "1")
  private Long id;

  @Schema(description = "입양 게시글 ID", example = "10")
  private Long adoptionPostId;

  @Schema(description = "삭제 여부", example = "false")
  private boolean deleted;

  @Schema(description = "댓글 작성자 정보")
  private UserInfoDto writer;

  @Schema(description = "댓글 내용", example = "입양을 희망합니다.")
  private String content;

  @Schema(description = "댓글 이미지 URL 목록")
  private List<String> imageUrls;

  @Schema(description = "생성 시각", example = "2026-06-04T12:34:56")
  private LocalDateTime createdAt;

  @Schema(description = "수정 시각", example = "2026-06-04T13:34:56", nullable = true)
  private LocalDateTime modifiedAt;

  @Schema(description = "대댓글 목록")
  private List<AdoptionCommentResponseDto> children;

  public static AdoptionCommentResponseDto from(
      AdoptionComment entity,
      List<String> imageUrls,
      List<AdoptionCommentResponseDto> childrenDto) {
    UserInfoDto writerDto = UserInfoDto.from(entity.getWriter());

    if (entity.getDeletedAt() != null) {
      return AdoptionCommentResponseDto.builder()
          .id(entity.getId())
          .adoptionPostId(entity.getAdoptionPost().getId())
          .deleted(true)
          .writer(writerDto)
          .content("삭제된 댓글입니다")
          .imageUrls(List.of())
          .createdAt(entity.getCreatedAt())
          .modifiedAt(entity.getModifiedAt())
          .children(childrenDto)
          .build();
    }

    return AdoptionCommentResponseDto.builder()
        .id(entity.getId())
        .adoptionPostId(entity.getAdoptionPost().getId())
        .deleted(false)
        .writer(writerDto)
        .content(entity.getContent())
        .imageUrls(imageUrls)
        .createdAt(entity.getCreatedAt())
        .modifiedAt(entity.getModifiedAt())
        .children(childrenDto)
        .build();
  }
}
