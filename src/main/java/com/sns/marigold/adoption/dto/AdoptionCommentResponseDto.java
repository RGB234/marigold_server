package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionComment;
import com.sns.marigold.user.dto.response.UserInfoDto;
import java.time.LocalDateTime;
import java.util.List;
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
public class AdoptionCommentResponseDto {

  private Long id;
  private Long adoptionPostId;
  private boolean deleted;
  private UserInfoDto writer;
  private String content;
  private List<String> imageUrls;
  private LocalDateTime createdAt;
  private LocalDateTime modifiedAt;
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
