package com.sns.marigold.auth.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
  ROLE_PERSON("일반 사용자 계정"),
  ROLE_ADMIN("관리자 계정");

  private final String description;

  @JsonCreator // 역직렬화. String -> Enum
  public static Role fromRole(String name) {
    return Arrays.stream(values())
      .filter(role -> role.name().equals(name))
      .findAny()
      .orElse(null);
  }
}
