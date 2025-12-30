package com.sns.marigold.auth.common.controller;

import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// OAuth2 로그인은 Spring security가 처리
// SecurityConfig 및 관련 코드 참조
// OAuth2 인증 엔드포인드 -> /oauth2/authorization/{registrationId} (기본값)
// /oauth2/authorization/kakao
// /oauth2/authorization/naver

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(Authentication authentication) {
    authService.logout(authentication);
    return ResponseEntity.ok().body(Map.of("message", "logout success"));
  }

  @GetMapping("/status")
  public ResponseEntity<UserAuthStatusDto> getAuthStatus(Authentication authentication) {
    return ResponseEntity.ok().body(authService.getAuthStatus(authentication));
  }
}