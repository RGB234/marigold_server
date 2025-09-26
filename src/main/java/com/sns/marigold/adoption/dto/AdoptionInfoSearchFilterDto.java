package com.sns.marigold.adoption.dto;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.global.enums.Species;
import lombok.Getter;

@Getter
public class AdoptionInfoSearchFilterDto {

  @Enum(target = Species.class)
  private Species species;
  @Enum(target = Sex.class)
  private Sex sex;
}
