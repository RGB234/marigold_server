package com.sns.marigold.auth.oauth2.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;

@Component("naverLogoutHandler")
public class NaverLogoutHandler implements OAuth2LogoutHandler {
  @Value("${naver.api.logout-url}")
  private String logoutUrl;

  @Override
  public ResponseEntity<String> logout(OAuth2AuthorizedClient client) {
    // 네이버는 별도 로그아웃 API가 없다. 추측이지만 토큰 기반이라 자동 폐기될 듯.
    // 대신 네이버 로그인 연동을 끊는 것은 있다. (동시에 발급받은 accessToken, refreshToken 둘 다 폐기된다.)
    // https://developers.naver.com/docs/login/devguide/devguide.md#4-3-%EB%84%A4%EC%9D%B4%EB%B2%84-token-revocation%ED%86%A0%ED%81%B0-%ED%8F%90%EA%B8%B0
    return ResponseEntity.ok().body("logout");
  }
}
