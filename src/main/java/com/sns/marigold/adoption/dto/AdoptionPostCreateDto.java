package com.sns.marigold.adoption.dto;

import java.util.Collections;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.annotation.EnumType;
import com.sns.marigold.global.annotation.ValidImageCount;
import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.global.validator.ImageCountValidatable;
import com.sns.marigold.user.entity.User;

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
@ValidImageCount(
    min = ValidationPolicy.AdoptionPost.IMAGE_MIN_COUNT,
    max = ValidationPolicy.AdoptionPost.IMAGE_MAX_COUNT)
@Schema(description = "입양 게시글 생성 요청")
public class AdoptionPostCreateDto implements ImageCountValidatable {

  @Schema(description = "동물 종", example = "DOG")
  @NotNull(message = "값이 비어있습니다.")
  @EnumType(target = Species.class)
  private Species species;

  @Schema(description = "나이", example = "3", minimum = "0")
  @NotNull(message = "값이 비어있습니다.")
  @Min(value = ValidationPolicy.AdoptionPost.AGE_MIN, message = "나이는 0 이상이어야 합니다.")
  @Builder.Default
  private Integer age = 0;

  @Schema(description = "성별", example = "MALE")
  @NotNull(message = "값이 비어있습니다.")
  @EnumType(target = Sex.class)
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
  @EnumType(target = Neutering.class)
  private Neutering neutering;

  @Schema(description = "특징 설명. 20자 이상 500자 이하", example = "사람을 좋아하고 산책을 좋아합니다.")
  @NotBlank(message = "값이 비어있습니다.")
  @Size(
      min = ValidationPolicy.AdoptionPost.FEATURES_MIN_LENGTH,
      max = ValidationPolicy.AdoptionPost.FEATURES_MAX_LENGTH,
      message = "20자 이상 500자 이하여야 합니다.")
  private String features;

  @Schema(description = "업로드할 이미지 파일 목록", type = "string", format = "binary")
  @ValidImageFiles()
  private List<MultipartFile> images;

  public List<String> getImagesToKeep() {
    return Collections.emptyList();
  }

  @Override
  public List<MultipartFile> getImages() {
    return images != null ? images : java.util.Collections.emptyList();
  }

  /*
  이미지는 Entity에서 setter 메서드로 설정
   */
  public AdoptionPost toEntity(User writer) {
    return AdoptionPost.builder()
        .writer(writer)
        .species(this.species)
        .title(this.title)
        .age(this.age)
        .sex(this.sex)
        .area(this.area)
        .weight(this.weight)
        .neutering(this.neutering)
        .features(this.features)
        .build();
  }
}
