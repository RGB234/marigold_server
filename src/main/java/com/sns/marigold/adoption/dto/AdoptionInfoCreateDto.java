package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AdoptionInfoCreateDto {

  @NotNull
  @Enum(target = Species.class)
  private Species species;

  @NotNull
  @Min(0)
  @Builder.Default
  private Integer age = 0;

  @Enum(target = Sex.class)
  private Sex sex;

  @NotBlank(message = "값이 비어있습니다.")
  private String area;

  @NotBlank(message = "값이 비어있습니다.")
  @Size(max = 16, message = "16자 이하여야 합니다.")
  private String title;

  @Min(0)
  @Builder.Default
  private Double weight = 0.0;

  @Enum(target = Neutering.class)
  private Neutering neutering;

  private String features;

  @Schema(description = "업로드할 이미지 파일들", type = "string", format = "binary")
  private List<MultipartFile> images;

  /*
  이미지는 Entity에서 setter 메서드로 설정
   */
  public AdoptionInfo toEntity(User writer) {
    return AdoptionInfo.builder()
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

// .images(images)
// .status(this.status)