package com.sns.marigold.auth;

import java.util.Map;
import lombok.Getter;

@Getter
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

  private final String sub;
  private final String email;

  GoogleOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes);
    this.sub = attributes.get("sub").toString();
    this.email = attributes.get("email").toString();
  }

  @Override
  public String getName() {
    return sub;
  }

  @Override
  public String getEmail() {
    return email;
  }
}
