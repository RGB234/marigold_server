package com.sns.marigold.auth.oauth2;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

public class OAuth2UserInfoFactory {

  public static OAuth2UserInfo getOAuth2UserInfo(
      ProviderInfo providerInfo, Map<String, Object> attributes) {
    switch (providerInfo) {
      case KAKAO -> {
        return new KakaoOAuth2UserInfo(attributes);
      }
      case NAVER -> {
        return new NaverOAuth2UserInfo(attributes);
      }
    }
    throw new OAuth2AuthenticationException(
        new OAuth2Error(
            AuthError.INVALID_PROVIDER.code(), AuthError.INVALID_PROVIDER.publicMessage(), null));
  }
}
