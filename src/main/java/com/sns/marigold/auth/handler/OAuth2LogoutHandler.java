package com.sns.marigold.auth.handler;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

public interface OAuth2LogoutHandler {

  void logout(OAuth2AuthorizedClient client);
}
