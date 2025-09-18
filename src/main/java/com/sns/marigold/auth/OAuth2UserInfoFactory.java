package com.sns.marigold.auth;

import com.sns.marigold.global.enums.ProviderInfo;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class OAuth2UserInfoFactory {

  public static OAuth2UserInfo getOAuth2UserInfo(
    ProviderInfo providerInfo, Map<String, Object> attributes) {
    switch (providerInfo) {
      case GOOGLE -> {
        return new GoogleOAuth2UserInfo(attributes);
      }
      case KAKAO -> {
        return new KakaoOAuth2UserInfo(attributes);
      }
      case NAVER -> {
        return new NaverOAuth2UserInfo(attributes);
      }
    }
    throw new OAuth2AuthenticationException("INVALID PROVIDER TYPE");
  }
}
