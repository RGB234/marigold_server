package com.sns.marigold.adoption.dto;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.global.enums.Species;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdoptionInfoCreateDto {

  @NotNull
  @Enum(target = Species.class)
  @Enumerated(EnumType.STRING)
  private Species species;

  @Size(min = 1, max = 30)
  private String name;

  @Min(0)
  private int age;

  @Enum(target = Sex.class)
  @Enumerated(EnumType.STRING)
  private Sex sex;

  @NotNull
  private String location;

  @Min(0)
  private Double weight;

  private Boolean neutering; // nullable
  
  private String features;
}
