package com.sns.marigold.adoption.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "동물 성별. MALE=남아, FEMALE=여아, UNKNOWN=불명, OTHER=기타")
@Getter
@AllArgsConstructor
public enum Sex {
  @Schema(description = "남아")
  MALE("남아", "MALE"),
  @Schema(description = "여아")
  FEMALE("여아", "FEMALE"),
  @Schema(description = "불명")
  UNKNOWN("불명", "UNKNOWN"),
  @Schema(description = "기타")
  OTHER("기타", "OTHER");

  private final String name;
  @JsonValue private final String value;
}
