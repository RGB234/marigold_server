package com.sns.marigold.auth;

import java.util.Map;

public abstract class OAuth2UserInfo {
  private final Map<String, Object> attributes;

  public OAuth2UserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public abstract String getName();

  public abstract String getEmail();
}
