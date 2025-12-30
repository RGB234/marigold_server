package com.sns.marigold.auth.oauth2.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthResponseCode;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * 로그인 전용 OAuth2UserService
 * 계정이 반드시 존재해야 하며, 없으면 예외를 발생시킵니다.
 */
@Service("oAuth2UserServiceForLogin")
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserServiceForLogin implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    Map<String, Object> attributes = oAuth2User.getAttributes();

    String providerCode = userRequest.getClientRegistration().getRegistrationId();
    ProviderInfo providerInfo = ProviderInfo.fromString(providerCode);

    OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerInfo, attributes);

    String providerId = oAuth2UserInfo.getName();

    Optional<User> userOptional = userRepository.findByProviderInfoAndProviderId(providerInfo, providerId);
    Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name()));
    if (userOptional.isPresent()) {
      User user = userOptional.get();
      return new CustomPrincipal(user.getId(), oAuth2UserInfo, authorities);
    } else {
      // 우리 서비스에 없는 사용자는 예외를 발생시킴
      throw new OAuth2AuthenticationException(
          new OAuth2Error(
              AuthResponseCode.USER_NOT_REGISTERED.getCode(),
              AuthResponseCode.USER_NOT_REGISTERED.getDescription(),
              null));
    }
  }
}
