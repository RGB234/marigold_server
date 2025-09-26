package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.global.enums.Neutering;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.global.enums.Species;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionDetailedInfoResponseDto {

  private Long id;

  private UUID writerId;

  private LocalDateTime createdAt;

  private LocalDateTime modifiedAt;

  private Species species;

  private String name;

  private Integer age;

  private Sex sex;

  private String area;

  private Double weight;

  private Neutering neutering;

  private String features;

  public static AdoptionDetailedInfoResponseDto from(AdoptionInfo adoptionInfo) {
    return AdoptionDetailedInfoResponseDto.builder()
      .id(adoptionInfo.getId())
      .writerId(adoptionInfo.getWriter().getId())
      .createdAt(adoptionInfo.getCreatedAt())
      .modifiedAt(adoptionInfo.getModifiedAt())
      .species(adoptionInfo.getSpecies())
      .name(adoptionInfo.getName())
      .age(adoptionInfo.getAge())
      .sex(adoptionInfo.getSex())
      .area(adoptionInfo.getArea())
      .weight(adoptionInfo.getWeight())
      .neutering(adoptionInfo.getNeutering())
      .features(adoptionInfo.getFeatures())
      .build();
  }
}
