package com.sns.marigold.adoption.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "입양 게시글 상태. PROCEEDING=모집중, RESERVED=예약중, COMPLETED=입양완료")
@Getter
@RequiredArgsConstructor
public enum AdoptionPostStatus {
  @Schema(description = "모집중")
  PROCEEDING("모집중"),
  @Schema(description = "예약중")
  RESERVED("예약중"),
  @Schema(description = "입양완료")
  COMPLETED("입양완료");

  private final String description;
}
