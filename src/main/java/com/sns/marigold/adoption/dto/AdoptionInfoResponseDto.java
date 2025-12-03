package com.sns.marigold.adoption.dto;

import java.util.UUID;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.global.enums.Species;
import com.sns.marigold.global.enums.Sex;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionInfoResponseDto {

  private UUID id;

  private Species species;

  private Integer age;

  private Sex sex;

  private String area;

  public static AdoptionInfoResponseDto from(AdoptionInfo adoptionInfo) {

    return AdoptionInfoResponseDto
      .builder()
      .id(adoptionInfo.getId())
      .species(adoptionInfo.getSpecies())
      .age(adoptionInfo.getAge())
      .sex(adoptionInfo.getSex())
      .area(adoptionInfo.getArea())
      .build();
  }
}