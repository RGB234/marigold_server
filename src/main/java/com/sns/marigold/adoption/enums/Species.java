package com.sns.marigold.adoption.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(
    description = "입양 동물 종. DOG=개, CAT=고양이, RODENTS=설치류, BIRDS=조류, REPTILES=파충류, FISH=어류, OTHER=기타")
@Getter
@AllArgsConstructor
public enum Species {
  @Schema(description = "개")
  DOG("개", "DOG"),

  @Schema(description = "고양이")
  CAT("고양이", "CAT"),
  @Schema(description = "설치류")
  RODENTS("설치류", "RODENTS"),
  @Schema(description = "조류")
  BIRDS("조류", "BIRDS"),
  @Schema(description = "파충류")
  REPTILES("파충류", "REPTILES"),
  @Schema(description = "어류")
  FISH("어류", "FISH"),
  @Schema(description = "기타")
  OTHER("기타", "OTHER");

  private final String name;
  @JsonValue private final String value;
}
