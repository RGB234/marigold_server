package com.sns.marigold.adoption.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "중성화 여부. YES=예, NO=아니오, UNKNOWN=불명")
@Getter
@AllArgsConstructor
public enum Neutering {
  @Schema(description = "중성화 완료")
  YES("예", "YES"),
  @Schema(description = "중성화 안 됨")
  NO("아니오", "NO"),
  @Schema(description = "불명")
  UNKNOWN("불명", "UNKNOWN");

  private String name;
  @JsonValue private String value;
}
