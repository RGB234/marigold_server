package com.sns.marigold.adoption.dto;

import java.util.UUID;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.Neutering;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.global.enums.Species;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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
  @Size(min = 2, max = 12, message = "2자 이상 12자 이하여야 합니다.")
  private String name;

  @Min(0)
  @Builder.Default
  private Double weight = 0.0;

  @Enum(target = Neutering.class)
  private Neutering neutering;

  private String features;

  public AdoptionInfo toEntity(UUID writerId) {
    return AdoptionInfo.builder()
            .writerId(writerId)
            .species(this.species) // getSpecies() 대신 필드 직접 접근 가능 (같은 클래스 내라면) 또는 getter 사용
            .name(this.name)
            .age(this.age)
            .sex(this.sex)
            .area(this.area)
            .weight(this.weight)
            .neutering(this.neutering)
            .features(this.features)
            .build();
}
}
