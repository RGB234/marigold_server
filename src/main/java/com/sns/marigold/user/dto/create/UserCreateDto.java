package com.sns.marigold.user.dto.create;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.annotation.Enum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {
  @Enum(target = ProviderInfo.class)
  @NotNull(message = "소셜 로그인 제공자는 필수입니다.")
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @NotBlank(message = "소셜 로그인 계정 ID는 필수입니다.")
  private String providerId; // 소셜로그인 계정 id
}
