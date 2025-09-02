package com.sns.marigold.user.dto;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.UserEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class UserCreateDTO {

  @NotBlank
  @Enum(target = ProviderInfo.class)
  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @NotBlank
  private String providerId; // 소셜로그인 계정 id

  @NotBlank
  @Enum(target = Role.class)
  @Enumerated(EnumType.STRING)
  private Role role;

  @NotBlank
  @Size(min = 3, max = 12)
  private String username;

  public UserEntity toUserEntity() {
    return UserEntity.builder()
      .providerInfo(providerInfo)
      .providerId(providerId)
      .role(role)
      .username(username)
      .build();
  }
}
