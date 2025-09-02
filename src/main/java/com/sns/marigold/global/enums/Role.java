package com.sns.marigold.global.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {

  //    ADMIN("관리자", "ADMIN"),
  //    USER("일반유저", "USER"),
  //    NOT_REGISTERED("미등록(소셜로그인 신규회원)", "NOT_REGISTERED");
  ROLE_ADMIN("관리자", "ADMIN"),
  ROLE_USER("일반유저", "USER"),
  ROLE_NOT_REGISTERED("미등록(소셜로그인 신규회원)", "NOT_REGISTERED");

  private final String name;
  @JsonValue private final String value;

  @JsonValue
  public static Role fromRole(String val) {
    return Arrays.stream(values())
        .filter(role -> role.getValue().equals(val))
        .findAny()
        .orElse(null);
  }
}
