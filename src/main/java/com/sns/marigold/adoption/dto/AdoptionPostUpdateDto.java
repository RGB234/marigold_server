package com.sns.marigold.adoption.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.global.validation.enumvalue.EnumValue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "입양 게시글 수정 요청")
public class AdoptionPostUpdateDto {

  @Schema(description = "동물 종", example = "DOG")
  @NotNull(message = "값이 비어있습니다.")
  @EnumValue(target = Species.class)
  private Species species;

  @Schema(description = "나이", example = "3", minimum = "0")
  @NotNull(message = "값이 비어있습니다.")
  @Min(value = ValidationPolicy.AdoptionPost.AGE_MIN, message = "나이는 0 이상이어야 합니다.")
  @Builder.Default
  private Integer age = 0;

  @Schema(description = "성별", example = "MALE")
  @NotNull(message = "값이 비어있습니다.")
  @EnumValue(target = Sex.class)
  private Sex sex;

  @Schema(description = "지역", example = "서울")
  @NotBlank(message = "값이 비어있습니다.")
  private String area;

  @Schema(description = "게시글 제목. 최대 16자", example = "가족을 찾습니다")
  @NotBlank(message = "값이 비어있습니다.")
  @Size(max = ValidationPolicy.AdoptionPost.TITLE_MAX_LENGTH, message = "제목은 16자 이하여야 합니다.")
  private String title;

  @Schema(description = "몸무게(kg)", example = "4.5", minimum = "0")
  @NotNull(message = "값이 비어있습니다.")
  @Min(value = ValidationPolicy.AdoptionPost.WEIGHT_MIN, message = "무게는 0 이상이어야 합니다.")
  @Builder.Default
  private Double weight = 0.0;

  @Schema(description = "중성화 여부", example = "YES")
  @NotNull(message = "값이 비어있습니다.")
  @EnumValue(target = Neutering.class)
  private Neutering neutering;

  @Schema(description = "특징 설명. 20자 이상 500자 이하", example = "사람을 좋아하고 산책을 좋아합니다.")
  @NotBlank(message = "값이 비어있습니다.")
  @Size(
      min = ValidationPolicy.AdoptionPost.FEATURES_MIN_LENGTH,
      max = ValidationPolicy.AdoptionPost.FEATURES_MAX_LENGTH,
      message = "20자 이상 500자 이하여야 합니다.")
  private String features;

  // 이전 + 추가 이미지 파일 갯수 >= 1
  @Schema(description = "유지할 기존 이미지 파일명 목록", nullable = true, example = "[\"images/old.jpg\"]")
  private List<String> imagesToKeep;

  @Schema(description = "새로 업로드할 이미지 파일 목록", type = "string", format = "binary", nullable = true)
  private List<MultipartFile> images;
}
