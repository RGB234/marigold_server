package com.sns.marigold.auth.oauth2.service;

import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.PersonalUserPrincipal;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.user.dto.PersonalUserCreateDto;
import com.sns.marigold.user.entity.PersonalUser;
import com.sns.marigold.user.repository.PersonalUserRepository;
import com.sns.marigold.user.service.PersonalUserService;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final PersonalUserService personalUserService;
  private final PersonalUserRepository personalUserRepository;


  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

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

    String providerCode = userRequest.getClientRegistration().getRegistrationId(); // "google", ...
    ProviderInfo providerInfo = ProviderInfo.fromString(providerCode);

    OAuth2UserInfo oAuth2UserInfo =
      OAuth2UserInfoFactory.getOAuth2UserInfo(providerInfo, attributes);

    String providerId = oAuth2UserInfo.getName();

    PersonalUser user = getUser(providerInfo, providerId);

    return new PersonalUserPrincipal(user, attributes, userNameAttributeName);
  }

  private PersonalUser getUser(ProviderInfo providerInfo, String providerId) {
    Optional<PersonalUser> optionalUser = personalUserRepository.findByProviderInfoAndProviderId(
      providerInfo, providerId);
    if (optionalUser.isEmpty()) {
      // 자동 계정 생성
      PersonalUserCreateDto dto = PersonalUserCreateDto.builder()
        .providerInfo(providerInfo)
        .providerId(providerId)
        .build();
//      PersonalUserResponseDto responseDto = userService.create(dto);
      UUID uid = personalUserService.create(dto);

//      return userService.findById(responseDto.getId());
      return personalUserService.findById(uid);
    }
    return optionalUser.get();

  }
}
