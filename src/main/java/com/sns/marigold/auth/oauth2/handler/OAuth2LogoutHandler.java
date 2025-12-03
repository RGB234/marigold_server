package com.sns.marigold.auth.oauth2.handler;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

public interface OAuth2LogoutHandler {
  ResponseEntity<String> logout(OAuth2AuthorizedClient client);
}
