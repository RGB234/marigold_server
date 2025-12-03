package com.sns.marigold.auth.oauth2;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;
//  private final ProviderInfo providerInfo = ProviderInfo.KAKAO;

  KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes, ProviderInfo.KAKAO);
    // https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#user-info-list
    this.id = attributes.get(getProviderInfo().getAttributeKey() + ".id").toString();
    this.email = attributes.get(getProviderInfo().getAttributeKey() + ".email").toString();
//    this.email = null;
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
