package com.sns.marigold.adoption.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Neutering {
  YES("예", "YES"),
  NO("아니오", "NO"),
  UNKNOWN("불명", "UNKNOWN");

  private String name;
  @JsonValue
  private String value;
}
