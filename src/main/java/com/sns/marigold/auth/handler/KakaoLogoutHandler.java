package com.sns.marigold.auth.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("kakaoLogoutHandler")
public class KakaoLogoutHandler implements OAuth2LogoutHandler {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${kakao.api.logout-url}")
  private String logoutUrl;

  @Override
  public void logout(OAuth2AuthorizedClient client) {
    // 카카오 엑세스 토큰 및 리프레쉬 토큰 폐기
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(client.getAccessToken().getTokenValue());
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    restTemplate.postForEntity(logoutUrl, entity, String.class);
  }
}
