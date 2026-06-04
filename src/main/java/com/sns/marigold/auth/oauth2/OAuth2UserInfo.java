package com.sns.marigold.auth.oauth2;

import java.util.Map;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

public abstract class OAuth2UserInfo {

  private final Map<String, Object> attributes;
  private final ProviderInfo providerInfo;

  public OAuth2UserInfo(Map<String, Object> attributes, ProviderInfo providerInfo) {
    this.attributes = attributes;
    this.providerInfo = providerInfo;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public ProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public abstract String getName();

  public abstract String getEmail();
}
