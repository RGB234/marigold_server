package com.sns.marigold.auth.service;

import com.sns.marigold.auth.PersonalUserPrincipal;
import com.sns.marigold.auth.handler.OAuth2LogoutHandler;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.service.InstitutionUserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
  private final InstitutionUserService userService;
  private final PasswordEncoder passwordEncoder; // SecurityConfig 에서 Bean에 PasswordEncoder 등록
  private final Map<String, OAuth2LogoutHandler> handlers;

  public AuthService(
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
    InstitutionUserService userService,
    PasswordEncoder passwordEncoder,
    List<OAuth2LogoutHandler> handerList) {
    this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.handlers = handerList.stream().collect(Collectors.toMap(
      h -> h.getClass().getAnnotation(Component.class).value(),
      h -> h
    ));
  }

  // OAuth2 로그인 -> Spring security 에서 처리 (SecurityConfig 및 관련코드 참조)

  // Email 로그인
  public ResponseEntity<Map<String, Object>> login(HttpServletRequest request,
    HttpServletResponse response, String email,
    String password) {
    InstitutionUser user;
    try {
      user = userService.findByEmail(email);
    } catch (EntityNotFoundException e) {
      Map<String, Object> responseBody = Map.of(
        "isAuthenticated", false,
        "uid", "",
        "role", "",
        "message", "Email is not matched."
      );
      return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(responseBody);
    }

    if (user != null && passwordEncoder.matches(password, user.getPassword())) {
      // 로그인 성공
      String uid = user.getId().toString();
//      String role = user.getRole().toString();

      // 세션에 최소한의 사용자 정보 저장
      HttpSession session = request.getSession(); // 세션 생성 (Tomcat)
      session.setAttribute("uid", uid);
      session.setAttribute("role", user.getRole().toString());

      Map<String, Object> responseBody = Map.of(
        "isAuthenticated", true,
        "uid", uid
      );
      return ResponseEntity.ok(responseBody);

    } else {
      Map<String, Object> responseBody = Map.of(
        "isAuthenticated", false,
        "uid", "",
        "message", "Password is not matched."
      );
      return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(responseBody);
    }
  }

  // 로그인 타입(OAuth2, Email)과 무관하게 공통사용
  public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
    HttpServletResponse response, Authentication authentication) {
    HttpSession session = request.getSession(false);
    assert session != null;

    // 소셜로그인일 경우 발급받은 accessToken & refreshToken 폐기 요청 전송
    String userRole = session.getAttribute("role").toString();
    if (userRole.equals(Role.ROLE_PERSON.name())) {
      PersonalUserPrincipal userPrincipal = (PersonalUserPrincipal) authentication.getPrincipal();
      String registrationId = userPrincipal.getUser().getProviderInfo().toString().toLowerCase();
      OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient(
        registrationId,
        authentication.getName()
      );
      String provider = client.getClientRegistration().getRegistrationId(); // kakao, naver
      OAuth2LogoutHandler handler = handlers.get(
        provider + "LogoutHandler"); // kakaoLogoutHandler, naverLogoutHandler

      if (handler != null) {
        handler.logout(client);
      }
    }

    // 세션 삭제
    session.invalidate();

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("isAuthenticated", false);
    return ResponseEntity.ok(responseBody);
  }

  public ResponseEntity<Map<String, Object>> checkAuth(
    String sid, HttpServletRequest request) {
    Map<String, Object> response = new HashMap<>();

    HttpSession session = request.getSession(false); // 없으면 새로 생성 X

    if (session != null && session.getId().equals(sid) && session.getAttribute("uid") != null) {
      // 세션 저장소 내에 존재하는 세션 ID
//      String uid = (String) session.getAttribute("uid");
//      String role = (String) session.getAttribute("role");

      response.put("isAuthenticated", true);
      return ResponseEntity.ok(response);
    } else {
      response.put("isAuthenticated", false);
      response.put("message", "Session does not exist or expired");
      return ResponseEntity.ok(response);
    }
  }
}
