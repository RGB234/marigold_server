package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;

import com.sns.marigold.adoption.entity.AdoptionInfo;
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

  private boolean completed;

  private LocalDateTime createdAt;

  public static AdoptionInfoResponseDto from(AdoptionInfo adoptionInfo) {
    return AdoptionInfoResponseDto
        .builder()
        .id(adoptionInfo.getId())
        .title(adoptionInfo.getTitle())
        .species(adoptionInfo.getSpecies())
        .age(adoptionInfo.getAge())
        .sex(adoptionInfo.getSex())
        .area(adoptionInfo.getArea())
        .imageUrl(adoptionInfo.getImages().isEmpty() ? null : adoptionInfo.getImages().get(0).getStoreFileName())
        .completed(adoptionInfo.isCompleted())
        .createdAt(adoptionInfo.getCreatedAt())
        .build();
  }
}