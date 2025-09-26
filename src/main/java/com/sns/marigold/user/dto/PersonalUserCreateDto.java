package com.sns.marigold.user.dto;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.ProviderInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PersonalUserCreateDto {

//  private Role role;

  @Enum(target = ProviderInfo.class)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @NotBlank
  private String providerId; // 소셜로그인 계정 id

//  @Builder
//  public PersonalUserCreateDto(ProviderInfo providerInfo, String providerId) {
//    this.providerInfo = providerInfo;
//    this.providerId = providerId;
//    this.role = Role.ROLE_PERSON;
//  }
}
