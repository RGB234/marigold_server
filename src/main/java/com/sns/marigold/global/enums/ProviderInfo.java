package com.sns.marigold.global.enums;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
//@RequiredArgsConstructor
@AllArgsConstructor
public enum ProviderInfo {
  GOOGLE(null, "sub", "email"),
  KAKAO("kakao_account", "id", "email"),
  NAVER("response", "id", "email");

  private final String attributeKey; // 소셜로부터 받은 데이터 parsing 을 위한 key
  private final String providerCode; // 소셜 로그인 종류 Code
  private final String providerId; // 소셜 로그인 사용자 정보 ID

  public static ProviderInfo fromString(String providerCode) {
    String upperCastedProvider = providerCode.toUpperCase();

    return Arrays.stream(ProviderInfo.values())
      .filter(p -> p.name().equals(upperCastedProvider))
      .findFirst()
      .orElseThrow(); // NoSuchElementException
  }
}
