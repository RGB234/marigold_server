package com.sns.marigold.auth.oauth2.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
//@RequiredArgsConstructor
@AllArgsConstructor
public enum ProviderInfo {
  GOOGLE("sub", "GOOGLE"),
  KAKAO("kakao_account", "KAKAO"),
  NAVER("response", "NAVER");

  private final String attributeKey; // 소셜로그인 공급자가 전달한 응답JSON 중 유저정보를 저장하는 속성(attribute) 이름
  private final String providerCode; // 소셜 로그인 종류 Code
//  private final String providerId; // 소셜 로그인 사용자 정보 ID

  public static ProviderInfo fromString(String providerCode) {
    String upperCastedProvider = providerCode.toUpperCase();

    return Arrays.stream(ProviderInfo.values())
        .filter(p -> p.name().equals(upperCastedProvider))
        .findFirst()
        .orElseThrow(); // NoSuchElementException
  }
}
