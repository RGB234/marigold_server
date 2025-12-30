package com.sns.marigold.auth.oauth2;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;

  NaverOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes, ProviderInfo.NAVER);
//    https://developers.naver.com/docs/login/profile/profile.md
    Object rawUserInfo = attributes.get(getProviderInfo().getAttributeKey());
    if (!(rawUserInfo instanceof Map<?, ?> userInfo)) {
      throw new IllegalArgumentException("네이버 사용자 정보 형식이 올바르지 않습니다.");
    }
    this.id = String.valueOf(userInfo.get("id"));
    this.email = ""; // 네이버 소셜 로그인 이메일 정보 사용 안함
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
