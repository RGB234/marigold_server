package com.sns.marigold.adoption.dto;

import java.util.UUID;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.global.enums.Species;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdoptionInfoSearchFilterDto {

  private UUID writerId;

  @Enum(target = Species.class)
  private Species species;
  
  @Enum(target = Sex.class)
  private Sex sex;
}
