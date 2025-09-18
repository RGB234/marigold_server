package com.sns.marigold.global.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
  ROLE_ADMIN("관리자", "ROLE_ADMIN"),
  ROLE_PERSON("개인", "ROLE_PERSON"),
  ROLE_INSTITUTION("조직", "ROLE_INSTITUTION");

  private final String name;
  @JsonValue // Role 직렬화시 value 값 사용
  private final String value;

  @JsonCreator // 역직렬화
  public static Role fromRole(String val) {
    return Arrays.stream(values())
      .filter(role -> role.getValue().equals(val))
      .findAny()
      .orElse(null);
  }
}
