package com.sns.marigold.auth.common.service;

import com.sns.marigold.auth.form.CustomUserDetails;
import com.sns.marigold.auth.oauth2.PersonalUserOAuth2Principal;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.oauth2.handler.OAuth2LogoutHandler;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.service.InstitutionUserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
  private final InstitutionUserService institutionUserService;
  private final PasswordEncoder passwordEncoder; // PasswordEncoderConfig
  private final Map<String, OAuth2LogoutHandler> handlers;

  public AuthService(
      OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
      InstitutionUserService institutionUserService,
      PasswordEncoder passwordEncoder,
      List<OAuth2LogoutHandler> handerList) {
    this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    this.institutionUserService = institutionUserService;
    this.passwordEncoder = passwordEncoder;
    this.handlers = handerList.stream().collect(Collectors.toMap(
        h -> h.getClass().getAnnotation(Component.class).value(),
        h -> h
    ));
  }

  // OAuth2 로그인 -> Spring security 에서 처리 (SecurityConfig 및 관련코드 참조)

  // Email 로그인
  public boolean institutionUserLogin(
      HttpServletRequest request,
      String username, String password) {
    InstitutionUser user;
    try {
      user = institutionUserService.findByUsername(username);
    } catch (EntityNotFoundException e) {
      log.error("username not found: {}", username);
      return false;
    }

    if (user != null && passwordEncoder.matches(password, user.getPassword())) {
      // 로그인 성공

      // 세션에 최소한의 사용자 정보 저장
//      String uid = user.getId().toString();
      HttpSession session = request.getSession(); // 세션 생성 (Tomcat)
//      session.setAttribute("uid", uid);
//
      // SecurityContext
      SecurityContext context = SecurityContextHolder.createEmptyContext();

      List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(Role.ROLE_INSTITUTION.getValue()));

//      CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getUsername(), user.getPassword(), authorities);
      CustomUserDetails userDetails = CustomUserDetails.builder()
          .uid(user.getId())
          .username(user.getUsername())
          .password(user.getPassword())
          .authorities(authorities)
          .build();

      Authentication authentication = new UsernamePasswordAuthenticationToken(
          userDetails,
          null, // 인증 성공 후이므로 null
          authorities
      );

      context.setAuthentication(authentication);
      // 현재 스레드에 적용
      SecurityContextHolder.setContext(context);

      // 영속성 확보
      session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

      return true;

    } else {
      log.error("Password not matched: {}", username);
      return false;
    }
  }

  // 로그인 방식(OAuth2, Email/PW)과 무관하게 공통사용
  public void logout(HttpServletRequest request,
                     Authentication authentication) {

    UserAuthStatusDto userAuthStatus = getAuthStatus(request);
    List<String> roles = userAuthStatus.getAuthorities();

    HttpSession session = request.getSession(false);
    if (session != null) {
      // 세션 삭제
      session.invalidate();
    }

    // 소셜로그인일 경우 소셜로그인 Provider(Naver, Kakao 등)로부터 발급받은 accessToken & refreshToken 폐기 요청 전송
    if (roles.contains("ROLE_PERSON")) {
      PersonalUserOAuth2Principal userPrincipal = (PersonalUserOAuth2Principal) authentication.getPrincipal();
      String registrationId = userPrincipal.getUser().getProviderInfo().toString().toLowerCase();

      OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient(
          registrationId, // application-oauth.yml 에 등록한 이름
          authentication.getName()
      );
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
  }

  public UserAuthStatusDto getAuthStatus(
      HttpServletRequest request
  ) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean isAuthenticated = false;
    List<? extends GrantedAuthority> authorities = new ArrayList<>();
    if (authentication != null && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {

      isAuthenticated = true;
      authorities = authentication.getAuthorities().stream().toList();
    }

    HttpSession session = request.getSession(false);

    return new UserAuthStatusDto(isAuthenticated, authorities);
  }
}
