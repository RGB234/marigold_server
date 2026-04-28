package com.sns.marigold.auth.oauth2.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.service.AuthService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.dto.create.OAuth2SignupDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserService userService;
  private final AuthService authService;
  private final CookieManager cookieManager;
  private final JwtManager jwtManager;

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

    ServletRequestAttributes requestAttributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("INTERNAL_ERROR", "요청 정보를 가져올 수 없습니다.", null));
    }
    HttpServletRequest request = requestAttributes.getRequest();

    Cookie actionCookie =
        cookieManager.getCookie(
            request, HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_ACTION_COOKIE_NAME);
    boolean isLinkAction = actionCookie != null && "link".equals(actionCookie.getValue());

    if (isLinkAction) {
      Cookie refreshCookie = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);
      if (refreshCookie != null) {
        String refreshToken = refreshCookie.getValue();
        try {
          Claims claims = jwtManager.getClaims(refreshToken);
          Long userId = jwtManager.getUserId(claims);
          if (userId != null) {
            User currentUser = userService.findEntityById(userId);
            authService.checkUserStatus(currentUser);

            // 해당 소셜 계정이 이미 다른 사용자에게 연동되어 있는지 확인
            Optional<User> linkedUser =
                userService.findEntityByProviderInfoAndProviderId(providerInfo, providerId);
            if (linkedUser.isPresent()) {
              throw new OAuth2AuthenticationException(
                  new OAuth2Error("ALREADY_LINKED", "이미 연동된 소셜 계정입니다.", null));
            }

            userService.linkOAuth2(userId, providerInfo, providerId);
            Collection<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(currentUser.getRole().name()));
            return new CustomPrincipal(
                currentUser.getId(), authorities, attributes, AuthStatus.LINK_SUCCESS);
          }
        } catch (OAuth2AuthenticationException e) {
          throw e;
        } catch (Exception e) {
          // 갱신 토큰이 유효하지 않은 경우
          throw new OAuth2AuthenticationException(
              new OAuth2Error("INVALID_TOKEN", "유효하지 않은 인증 토큰입니다.", null));
        }
      } else {
        throw new OAuth2AuthenticationException(
            new OAuth2Error("UNAUTHORIZED", "로그인 상태가 아닙니다.", null));
      }
    }

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

      Long userId = authService.oauth2Signup(oAuth2SignupDto);

      Collection<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name()));

      return new CustomPrincipal(userId, authorities, attributes, AuthStatus.SIGNUP_SUCCESS);
    }
  }
}
