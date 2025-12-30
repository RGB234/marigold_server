package com.sns.marigold.auth.oauth2.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthResponseCode;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import com.sns.marigold.user.service.UserService;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
 * 회원가입 전용 OAuth2UserService
 * 계정이 없어도 예외를 발생시키지 않고, CustomPrincipal의 userId를 null로 반환합니다.
 * Handler에서 계정 존재 여부를 확인하여 회원가입을 진행합니다.
 */
@Service("oAuth2UserServiceForSignup")
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserServiceForSignup implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserRepository userRepository;
  private final UserService userService;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    Map<String, Object> attributes = oAuth2User.getAttributes();

    String providerCode = userRequest.getClientRegistration().getRegistrationId();
    ProviderInfo providerInfo = ProviderInfo.fromString(providerCode);

    OAuth2UserInfo oAuth2UserInfo =
        OAuth2UserInfoFactory.getOAuth2UserInfo(providerInfo, attributes);

    String providerId = oAuth2UserInfo.getName();

    // 계정 조회 (없어도 예외 발생하지 않음)
    Optional<User> userOptional = userRepository.findByProviderInfoAndProviderId(providerInfo, providerId);

    Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name()));

    if (userOptional.isPresent()) {
      // 계정이 이미 존재하는 경우 (Handler에서 에러 처리)
      throw new OAuth2AuthenticationException(
        new OAuth2Error(
            AuthResponseCode.USER_ALREADY_REGISTERED.getCode(),
            AuthResponseCode.USER_ALREADY_REGISTERED.getDescription(),
            null));
    } else {
      UUID userId = userService.createUser(
        UserCreateDto.builder()
            .providerInfo(providerInfo)
            .providerId(providerId)
            .build());

      return new CustomPrincipal(userId, oAuth2UserInfo, authorities);
    }
  }
}

