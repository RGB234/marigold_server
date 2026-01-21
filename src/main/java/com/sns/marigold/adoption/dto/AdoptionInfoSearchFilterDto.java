package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.annotation.Enum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdoptionInfoSearchFilterDto {
  @Enum(target = Species.class)
  private Species species;
  
  @Enum(target = Sex.class)
  private Sex sex;
}
