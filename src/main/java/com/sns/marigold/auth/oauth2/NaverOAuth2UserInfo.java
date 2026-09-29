package com.sns.marigold.auth.oauth2;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;

  NaverOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes, ProviderInfo.NAVER);
    //    https://developers.naver.com/docs/login/profile/profile.md
    Object rawUserInfo = attributes.get(getProviderInfo().getAttributeKey());
    if (!(rawUserInfo instanceof Map<?, ?> userInfo) || userInfo.get("id") == null) {
      throw invalidUserInfo();
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

  private OAuth2AuthenticationException invalidUserInfo() {
    return new OAuth2AuthenticationException(
        new OAuth2Error(
            AuthError.OAUTH2_USER_INFO_NOT_FOUND.code(),
            AuthError.OAUTH2_USER_INFO_NOT_FOUND.publicMessage(),
            null));
  }
}
