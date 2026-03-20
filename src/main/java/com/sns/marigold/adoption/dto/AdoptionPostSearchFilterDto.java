package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.global.annotation.EnumType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdoptionPostSearchFilterDto {
  @EnumType(target = Species.class)
  private Species species;
  
  @EnumType(target = Sex.class)
  private Sex sex;
}
