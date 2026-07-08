package com.sns.marigold.auth.oauth2.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.service.AuthService;
import com.sns.marigold.auth.common.service.RecentAuthService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import com.sns.marigold.auth.oauth2.OAuth2UserInfoFactory;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.error.exception.BusinessException;
import com.sns.marigold.user.dto.create.OAuth2SignupDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.service.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final UserService userService;
  private final AuthService authService;
  private final CookieManager cookieManager;
  private final JwtManager jwtManager;
  private final RecentAuthService recentAuthService;

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
      return linkOAuth2(providerInfo, providerId, request, attributes);
    } else {
      return loginOrSignupOAuth2User(providerInfo, providerId, attributes);
    }
  }

  private CustomPrincipal linkOAuth2(
      ProviderInfo providerInfo,
      String providerId,
      HttpServletRequest request,
      Map<String, Object> attributes) {
    Cookie refreshCookie = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);

    if (refreshCookie == null) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("UNAUTHORIZED", "로그인 상태가 아닙니다.", null));
    }

    String refreshToken = refreshCookie.getValue();
    Long userId;
    try {
      Claims claims = jwtManager.getClaims(refreshToken);
      userId = jwtManager.getUserId(claims);
    } catch (JwtException | IllegalArgumentException e) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("INVALID_TOKEN", "유효하지 않은 인증 토큰입니다.", null));
    }

    try {
      User currentUser = userService.findEntityById(userId);
      authService.checkUserStatus(currentUser);
      // 2차 인증 검사
      recentAuthService.validate(request, userId);

      userService.linkOAuth2(userId, providerInfo, providerId);

      Collection<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority(currentUser.getRole().name()));

      return new CustomPrincipal(
          currentUser.getId(), authorities, attributes, AuthStatus.LINK_SUCCESS);
    } catch (AuthException | UserException e) {
      throw toOAuth2Exception(e);
    }
  }

  private CustomPrincipal createLoginPrincipal(User user, Map<String, Object> attributes) {
    Collection<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority(user.getRole().name()));

    return new CustomPrincipal(
        user.getId(), authorities, attributes, user.getStatus().toAuthStatus());
  }

  private CustomPrincipal signupOAuth2User(
      ProviderInfo providerInfo, String providerId, Map<String, Object> attributes) {
    OAuth2SignupDto signupDto =
        OAuth2SignupDto.builder()
            .providerInfo(providerInfo)
            .providerId(providerId)
            .role(Role.ROLE_PERSON)
            .build();

    Long userId = authService.oauth2Signup(signupDto);

    Collection<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name()));

    return new CustomPrincipal(userId, authorities, attributes, AuthStatus.SIGNUP_SUCCESS);
  }

  private CustomPrincipal loginOrSignupOAuth2User(
      ProviderInfo providerInfo, String providerId, Map<String, Object> attributes) {
    return userService
        .findEntityByProviderInfoAndProviderId(providerInfo, providerId)
        .map(user -> createLoginPrincipal(user, attributes))
        .orElseGet(() -> signupOAuth2User(providerInfo, providerId, attributes));
  }

  private OAuth2AuthenticationException toOAuth2Exception(BusinessException e) {
    return new OAuth2AuthenticationException(
        new OAuth2Error(e.getErrorCode().getCode(), e.getMessage(), null));
  }
}
