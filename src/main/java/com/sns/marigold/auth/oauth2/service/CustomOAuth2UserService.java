package com.sns.marigold.auth.oauth2.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import jakarta.transaction.Transactional;

import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    Map<String, Object> attributes = oAuth2User.getAttributes();

    String providerCode = userRequest.getClientRegistration().getRegistrationId(); // "google", ...
    ProviderInfo providerInfo = ProviderInfo.fromString(providerCode);

    OAuth2UserInfo oAuth2UserInfo =
        OAuth2UserInfoFactory.getOAuth2UserInfo(providerInfo, attributes);

    Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name()));
    return new CustomPrincipal(oAuth2UserInfo, authorities);
  }
}
