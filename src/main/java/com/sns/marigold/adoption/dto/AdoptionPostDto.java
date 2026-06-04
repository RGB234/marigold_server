package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "입양 게시글 요약 응답")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionPostDto {

  @Schema(description = "입양 게시글 ID", example = "1")
  private Long id;

  @Schema(description = "게시글 제목", example = "가족을 찾습니다")
  private String title;

  @Schema(description = "동물 종", example = "DOG")
  private Species species;

  @Schema(description = "나이", example = "3")
  private Integer age;

  @Schema(description = "성별", example = "MALE")
  private Sex sex;

  @Schema(description = "지역", example = "서울")
  private String area;

  @Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg", nullable = true)
  @Setter
  @Getter
  private String imageUrl;

  @Schema(description = "게시글 상태", example = "OPEN")
  private AdoptionPostStatus status;

  @Schema(description = "생성 시각", example = "2026-06-04T12:34:56")
  private LocalDateTime createdAt;

  @Schema(description = "삭제 여부", example = "false")
  private boolean isDeleted;

  public static AdoptionPostDto from(AdoptionPost adoptionPost) {
    boolean deleted = adoptionPost.getDeletedAt() != null;
    String maskedTitle = deleted ? "삭제된 게시글입니다" : adoptionPost.getTitle();

    return AdoptionPostDto.builder()
        .id(adoptionPost.getId())
        .title(maskedTitle)
        .species(adoptionPost.getSpecies())
        .age(adoptionPost.getAge())
        .sex(adoptionPost.getSex())
        .area(adoptionPost.getArea())
        .imageUrl(
            deleted || adoptionPost.getImages().isEmpty()
                ? null
                : adoptionPost.getImages().get(0).getStoredFileName())
        .status(adoptionPost.getStatus())
        .createdAt(adoptionPost.getCreatedAt())
        .isDeleted(deleted)
        .build();
  }
}
