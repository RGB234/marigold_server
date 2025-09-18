package com.sns.marigold.auth.controller;

import com.sns.marigold.auth.dto.EmailLoginDto;
import com.sns.marigold.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// OAuth2 로그인은 Spring security가 처리
// SecurityConfig 및 관련 코드 참조
// OAuth2 인증 엔드포인드 -> /oauth2/authorization/{registrationId} (기본값)
// /oauth2/authorization/kakao
// /oauth2/authorization/naver

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(HttpServletRequest request,
    HttpServletResponse response, @RequestBody @Valid EmailLoginDto dto) {
    return authService.login(request, response, dto.getEmail(), dto.getPassword());
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
    HttpServletResponse response, Authentication authentication) {
    return authService.logout(request, response, authentication);
  }

  @GetMapping("/check-auth")
  public ResponseEntity<Map<String, Object>> checkAuth(
    // JSESSIONID는 Tomcat이 자동으로 관리
    @CookieValue(value = "JSESSIONID", required = false) String sid, HttpServletRequest request) {
    return authService.checkAuth(sid, request);
  }
}