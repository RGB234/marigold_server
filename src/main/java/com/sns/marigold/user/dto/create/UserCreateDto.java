package com.sns.marigold.user.dto.create;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.user.entity.User;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@NoArgsConstructor // Jackson을 위한 public 기본 생성자
public class UserCreateDto {
  @NotBlank
  private String nickname;

  @Enum(target = ProviderInfo.class)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @NotBlank
  private String providerId; // 소셜로그인 계정 id

  public User toEntity() {
    return User.builder()
        .nickname(this.getNickname())
        .providerInfo(this.providerInfo)
        .providerId(this.providerId)
        .build();
  }
}
