package com.sns.marigold.auth.common.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "사용자 권한. ROLE_PERSON=일반 사용자, ROLE_ADMIN=관리자")
@Getter
@AllArgsConstructor
public enum Role {
  @Schema(description = "일반 사용자 계정")
  ROLE_PERSON("일반 사용자 계정"),
  @Schema(description = "관리자 계정")
  ROLE_ADMIN("관리자 계정");

  private final String description;

  @JsonCreator // 역직렬화. String -> Enum
  public static Role fromRole(String name) {
    return Arrays.stream(values()).filter(role -> role.name().equals(name)).findAny().orElse(null);
  }
}
