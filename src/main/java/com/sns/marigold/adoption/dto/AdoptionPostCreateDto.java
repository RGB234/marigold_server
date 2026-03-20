package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.annotation.EnumType;
import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.global.annotation.ValidImageCount;
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

import java.util.Collections;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ValidImageCount(min = 1, max = 8)
public class AdoptionPostCreateDto implements ImageCountValidatable {

  @NotNull(message = "값이 비어있습니다.")
  @EnumType(target = Species.class)
  private Species species;

  @NotNull(message = "값이 비어있습니다.")
  @Min(value=0, message = "나이는 0 이상이어야 합니다.")
  @Builder.Default
  private Integer age = 0;

  @NotNull(message = "값이 비어있습니다.")
  @EnumType(target = Sex.class)
  private Sex sex;

  @NotBlank(message = "값이 비어있습니다.")
  private String area;

  @NotBlank(message = "값이 비어있습니다.")
  @Size(max = 16, message = "제목은 16자 이하여야 합니다.")
  private String title;

  @NotNull(message = "값이 비어있습니다.")
  @Min(value = 0, message = "무게는 0 이상이어야 합니다.")
  @Builder.Default
  private Double weight = 0.0;

  @NotNull(message = "값이 비어있습니다.")
  @EnumType(target = Neutering.class)
  private Neutering neutering;

  @NotNull(message = "값이 비어있습니다.")
  @Size(min = 20, max = 500, message = "20자 이상 500자 이하여야 합니다.")
  private String features;

  @Schema(description = "업로드할 이미지 파일들", type = "string", format = "binary")
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