package com.sns.marigold.auth.service;

import com.sns.marigold.auth.OAuth2UserInfo;
import com.sns.marigold.auth.OAuth2UserInfoFactory;
import com.sns.marigold.auth.RandomUsernameGenerator;
import com.sns.marigold.auth.UserPrincipal;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.UserCreateDTO;
import com.sns.marigold.user.entity.UserEntity;
import com.sns.marigold.user.repository.UserRepository;
import com.sns.marigold.user.service.UserService;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class CustomOAuth2UserService implements OAuth2UserService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final RandomUsernameGenerator usernameGenerator;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    log.info("oAuth2User : {}", oAuth2User);

    Map<String, Object> attributes = oAuth2User.getAttributes();

    //        for (String key : attributes.keySet()) {
    //            log.info("{} : {}", key, attributes.get(key));
    //        }

    String userNameAttributeName =
        userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

    //        log.info("userNameAttributeName : {}", userNameAttributeName);

    String providerCode = userRequest.getClientRegistration().getRegistrationId();
    //        log.info("providerCode : {}", providerCode);

    ProviderInfo providerInfo = ProviderInfo.fromProvider(providerCode);
    //        log.info("providerInfo : {}", providerInfo);

    OAuth2UserInfo oAuth2UserInfo =
        OAuth2UserInfoFactory.getOAuth2UserInfo(providerInfo, attributes);

    String providerId = oAuth2UserInfo.getName();
    String email = oAuth2UserInfo.getEmail();

    //        String providerId = attributes.get(userNameAttributeName).toString();
    //        String email = attributes.get("email").toString();

    UserEntity user = getUser(providerId, providerInfo, email);

    return new UserPrincipal(user, attributes, userNameAttributeName);
  }

  private UserEntity getUser(String providerId, ProviderInfo providerInfo, String email) {
    Optional<UserEntity> optionalUser = userRepository.findByProviderId(providerId);
    if (optionalUser.isEmpty()) {
      // 일단 계정 생성
      // 계정 생성 후 리다이렉션 되는 회원가입 페이지에서 추가 정보 입력
      UserCreateDTO userCreateDTO =
          UserCreateDTO.builder()
              .email(email)
              .role(Role.ROLE_NOT_REGISTERED)
              .providerId(providerId)
              .providerInfo(providerInfo)
              .username(usernameGenerator.generate())
              .build();

      userService.create(userCreateDTO);
      return userCreateDTO.toUserEntity();
    }
    return optionalUser.get();
  }
}
