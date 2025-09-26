package com.sns.marigold.global.enums;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex {
  MALE("수컷", "MALE"),
  FEMALE("암컷", "FEMALE"),
  UNKNOWN("모름", "UNKNOWN"),
  OTHER("기타", "OTHER");

  private final String name;
  @JsonValue
  private final String value;
}
