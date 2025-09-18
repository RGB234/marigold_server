package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.global.enums.Species;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.user.dto.UserResponseDto;
import com.sns.marigold.user.entity.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdoptionInfoResponseDto {

  private UserResponseDto writer;

  private LocalDateTime createdAt;

  private LocalDateTime modifiedAt;

  private Species species;

  private String name;

  private int age;

  private Sex sex;

  private String location;

  private Double weight;

  private boolean neutering;

  private String features;

  public static AdoptionInfoResponseDto fromAdoptionInfo(AdoptionInfo adoptionInfo) {
    User user = adoptionInfo.getWriter();

    return AdoptionInfoResponseDto
      .builder()
      .writer(UserResponseDto.fromUser(user))
      .createdAt(adoptionInfo.getCreatedAt())
      .modifiedAt(adoptionInfo.getModifiedAt())
      .species(adoptionInfo.getSpecies())
      .name(adoptionInfo.getName())
      .age(adoptionInfo.getAge())
      .sex(adoptionInfo.getSex())
      .location(adoptionInfo.getLocation())
      .weight(adoptionInfo.getWeight())
      .neutering(adoptionInfo.isNeutering())
      .features(adoptionInfo.getFeatures())
      .build();
  }
}

/*
writer createdAt modifiedAt species name age sex location weight neutering features
 */