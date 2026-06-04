package com.sns.marigold.auth.oauth2;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.error.ErrorCode;

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
            ErrorCode.AUTH_INVALID_PROVIDER.getCode(),
            ErrorCode.AUTH_INVALID_PROVIDER.getMessage(),
            null));
  }
}
