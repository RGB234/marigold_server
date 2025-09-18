package com.sns.marigold.auth;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;

  KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes);
    // https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#user-info-list
    this.id = attributes.get("id").toString();
//    this.email = attributes.get("kakao_account.email").toString();
    this.email = null;
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
