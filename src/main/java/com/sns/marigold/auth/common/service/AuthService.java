package com.sns.marigold.auth.common.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.oauth2.handler.OAuth2LogoutHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {
  private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
  private final Map<String, OAuth2LogoutHandler> handlers;

  public AuthService(
      OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
      List<OAuth2LogoutHandler> handerList) {
    this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    this.handlers = handerList.stream().collect(Collectors.toMap(
        h -> h.getClass().getAnnotation(Component.class).value(),
        h -> h));
  }

  // OAuth2 로그인 -> Spring security 에서 처리 (SecurityConfig & CustomOAuth2UserService
  // 참조)

  public void logout(Authentication authentication) {
    // JWT는 stateless이므로 서버에서 세션 삭제 불필요
    // 클라이언트에서 토큰 삭제 처리

    // 소셜로그인 Provider(Naver, Kakao 등)로부터 발급받은 accessToken & refreshToken 폐기 요청 전송
    CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();
    String registrationId = userPrincipal.getOAuth2UserInfo().getProviderInfo().name();

    OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient(
        registrationId, // application-oauth.yml 에 등록한 이름. kakao, naver 등
        authentication.getName());
    if (client != null) {
      OAuth2LogoutHandler handler = handlers.get(
          registrationId + "LogoutHandler");
      if (handler != null) {
        ResponseEntity<String> handlerLogoutResponse = handler.logout(client);
        log.info("OAuth2 Logout response: {}", handlerLogoutResponse);
      } else {
        log.warn("No handler found for ${}", registrationId);
      }
    } else {
      log.warn("No client found for ${}", registrationId);
    }
  }

  public UserAuthStatusDto getAuthStatus(
      Authentication authentication) {
    List<? extends GrantedAuthority> authorities = new ArrayList<>();
    boolean isAuthenticated = false;

    if (authentication != null) {
      CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();
      isAuthenticated = true;
      authorities = userPrincipal.getAuthorities().stream().toList();
    }

    return new UserAuthStatusDto(isAuthenticated, authorities);
  }
}
