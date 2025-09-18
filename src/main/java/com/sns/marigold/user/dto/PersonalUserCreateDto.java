package com.sns.marigold.user.dto;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.ProviderInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class PersonalUserCreateDto {

  @Enum(target = ProviderInfo.class)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @NotBlank
  private String providerId; // 소셜로그인 계정 id
}
