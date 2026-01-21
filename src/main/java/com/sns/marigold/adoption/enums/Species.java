package com.sns.marigold.adoption.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Species {
  DOG("개", "DOG"),

  CAT("고양이", "CAT"),
  RODENTS("설치류", "RODENTS"),
  BIRDS("조류", "BIRDS"),
  REPTILES("파충류", "REPTILES"),
  FISH("어류", "FISH"),
  OTHER("기타", "OTHER");

  private final String name;
  @JsonValue
  private final String value;
}
