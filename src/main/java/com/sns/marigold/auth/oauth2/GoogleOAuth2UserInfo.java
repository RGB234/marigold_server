package com.sns.marigold.auth.oauth2;

import java.util.Map;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import lombok.Getter;

@Getter
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

  private final String sub;
  private final String email;

  GoogleOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes, ProviderInfo.GOOGLE);
    this.sub = attributes.get(getProviderInfo().getAttributeKey()).toString();
    this.email = attributes.get(getProviderInfo().getAttributeKey() + ".email").toString();
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
