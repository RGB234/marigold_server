package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
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
public class AdoptionPostDto {

  private Long id;

  private String title;

  private Species species;

  private Integer age;

  private Sex sex;

  private String area;

  private String imageUrl;

  private AdoptionPostStatus status;

  private LocalDateTime createdAt;

  public static AdoptionPostDto from(AdoptionPost adoptionPost) {
    return AdoptionPostDto
        .builder()
        .id(adoptionPost.getId())
        .title(adoptionPost.getTitle())
        .species(adoptionPost.getSpecies())
        .age(adoptionPost.getAge())
        .sex(adoptionPost.getSex())
        .area(adoptionPost.getArea())
        .imageUrl(adoptionPost.getImages().isEmpty() ? null : adoptionPost.getImages().get(0).getStoredFileName())
        .status(adoptionPost.getStatus())
        .createdAt(adoptionPost.getCreatedAt())
        .build();
  }
}