package com.sns.marigold.auth.oauth2;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

  private final String id;
  private final String email;

  //  private final ProviderInfo providerInfo = ProviderInfo.KAKAO;

  KakaoOAuth2UserInfo(Map<String, Object> attributes) {
    super(attributes, ProviderInfo.KAKAO);
    // 카카오 계정 정보 사용 안함
    // Object rawUserInfo = attributes.get(getProviderInfo().getAttributeKey());
    // if (!(rawUserInfo instanceof Map<?, ?> userInfo)) {
    //   throw new IllegalArgumentException("카카오 사용자 정보 형식이 올바르지 않습니다.");
    // }
    // https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#user-info-list
    Object providerId = attributes.get("id");
    if (providerId == null) {
      throw invalidUserInfo();
    }
    this.id = String.valueOf(providerId); // 회원번호
    this.email = ""; // 카카오 소셜 로그인 이메일 정보 사용 안함
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
