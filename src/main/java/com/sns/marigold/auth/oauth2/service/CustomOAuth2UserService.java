package com.sns.marigold.auth.oauth2.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.dto.create.OAuth2SignupDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.enums.UserStatus;
import com.sns.marigold.user.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserService userService;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
    OAuth2User oAuth2User = delegate.loadUser(userRequest);

    Map<String, Object> attributes = oAuth2User.getAttributes();

    // OAuth2 인증 provider 정보 추출
    String providerCode = userRequest.getClientRegistration().getRegistrationId();
    
    ProviderInfo providerInfo = ProviderInfo.fromString(providerCode);
    // 해당 provider에서 제공하는 사용자 정보
    OAuth2UserInfo oAuth2UserInfo =
        OAuth2UserInfoFactory.getOAuth2UserInfo(providerInfo, attributes);

    String providerId = oAuth2UserInfo.getName();

    Optional<User> userOptional =
        userService.findEntityByProviderInfoAndProviderId(providerInfo, providerId);
    if (userOptional.isPresent()) { // 이미 존재하는 사용자라면 로그인처리

      // 예외 발생 대신 상태값을 리턴하여 핸들러에서 처리하도록 변경
      // authService.checkUserStatus(userOptional.get());

      User user = userOptional.get();

      Collection<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority(user.getRole().name()));

      AuthStatus authStatus;
      authStatus = user.getStatus().toAuthStatus();

      return new CustomPrincipal(user.getId(), authorities, attributes, authStatus);

    } else { // 존재하지 않는 사용자라면 회원가입 처리
      OAuth2SignupDto oAuth2SignupDto =
          OAuth2SignupDto.builder()
              .providerInfo(providerInfo)
              .providerId(providerId)
              .role(Role.ROLE_PERSON) // 기본 권한은 일반 사용자로 설정
              .build();

      Long userId = userService.createUser(oAuth2SignupDto);

      Collection<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name()));

      return new CustomPrincipal(userId, authorities, attributes, AuthStatus.SIGNUP_SUCCESS);
    }
  }
}
