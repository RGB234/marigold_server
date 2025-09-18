package com.sns.marigold.auth;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;

  NaverOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes);
//    https://developers.naver.com/docs/login/profile/profile.md
    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
    this.id = response.get("id").toString();
//    this.email = attributes.get("email").toString();
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
