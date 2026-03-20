package com.sns.marigold.adoption.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdoptionPostStatus {
  PROCEEDING("모집중"),
  RESERVED("예약중"),
  COMPLETED("입양완료");

  private final String description;
}
