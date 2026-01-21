package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.enums.AdoptionStatus;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;

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

  private Long id;

  private String title;

  private Species species;

  private Integer age;

  private Sex sex;

  private String area;

  private String imageUrl;

  private AdoptionStatus status;

  public static AdoptionInfoResponseDto from(AdoptionInfo adoptionInfo) {
    return AdoptionInfoResponseDto
        .builder()
        .id(adoptionInfo.getId())
        .title(adoptionInfo.getTitle())
        .species(adoptionInfo.getSpecies())
        .age(adoptionInfo.getAge())
        .sex(adoptionInfo.getSex())
        .area(adoptionInfo.getArea())
        .imageUrl(adoptionInfo.getImages().get(0).getImageUrl())
        .status(adoptionInfo.getStatus())
        .build();
  }
}