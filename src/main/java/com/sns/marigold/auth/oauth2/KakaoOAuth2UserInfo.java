package com.sns.marigold.auth.oauth2;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;
//  private final ProviderInfo providerInfo = ProviderInfo.KAKAO;

  KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes, ProviderInfo.KAKAO);
    // 카카오 계정 정보 사용 안함
    // Object rawUserInfo = attributes.get(getProviderInfo().getAttributeKey());
    // if (!(rawUserInfo instanceof Map<?, ?> userInfo)) {
    //   throw new IllegalArgumentException("카카오 사용자 정보 형식이 올바르지 않습니다.");
    // }
    // https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#user-info-list
    this.id = String.valueOf(attributes.get("id")); // 회원번호
    this.email = ""; // 카카오 소셜 로그인 이메일 정보 사용 안함
  }

  @Override
  public String getName() {
    return id;
  }

  @Override
  public String getEmail() {
    return email;
  }
}
