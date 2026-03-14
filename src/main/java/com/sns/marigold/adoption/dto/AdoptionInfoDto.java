package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;

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
public class AdoptionInfoDto {

  private Long id;

  private String title;

  private Species species;

  private Integer age;

  private Sex sex;

  private String area;

  private String imageUrl;

  private AdoptionStatus status;

  private LocalDateTime createdAt;

  public static AdoptionInfoDto from(AdoptionInfo adoptionInfo) {
    return AdoptionInfoDto
        .builder()
        .id(adoptionInfo.getId())
        .title(adoptionInfo.getTitle())
        .species(adoptionInfo.getSpecies())
        .age(adoptionInfo.getAge())
        .sex(adoptionInfo.getSex())
        .area(adoptionInfo.getArea())
        .imageUrl(adoptionInfo.getImages().isEmpty() ? null : adoptionInfo.getImages().get(0).getStoredFileName())
        .status(adoptionInfo.getStatus())
        .createdAt(adoptionInfo.getCreatedAt())
        .build();
  }
}