package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.annotation.EnumType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "입양 게시글 검색 조건")
@Getter
@Setter
public class AdoptionPostSearchFilterDto {
  @Schema(description = "동물 종 필터", example = "DOG", nullable = true)
  @EnumType(target = Species.class)
  private Species species;

  @Schema(description = "성별 필터", example = "MALE", nullable = true)
  @EnumType(target = Sex.class)
  private Sex sex;
}
