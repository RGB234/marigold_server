package com.sns.marigold.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
  ACTIVE("정상"),
  DELETED("탈퇴"),
  BANNED("이용 제한"),
  SLEEP("휴면");

  private final String description;
}
