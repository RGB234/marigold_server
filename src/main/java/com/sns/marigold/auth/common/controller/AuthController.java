package com.sns.marigold.auth.common.controller;

import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.service.AuthService;
import com.sns.marigold.auth.form.dto.FormLoginDto;
import jakarta.persistence.GeneratedValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private Environment env;

  @PostMapping("/login/institution")
  public ResponseEntity<Map<String, Object>> login(HttpServletRequest request,
                                                   @RequestBody @Valid FormLoginDto dto) {

    if (authService.institutionUserLogin(request, dto.getUsername(), dto.getPassword())) {
      return ResponseEntity.ok().body(Map.of("message", "로그인 성공"));
    } else {
      return ResponseEntity.badRequest().body(Map.of("message", "잘못된 이메일 혹은 비밀번호"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
                                                    Authentication authentication) {
    authService.logout(request, authentication);
    return ResponseEntity.ok().body(Map.of("message", "logout success"));
  }

  @GetMapping("/status")
  public ResponseEntity<UserAuthStatusDto> getAuthStatus(HttpServletRequest request) {
    return ResponseEntity.ok().body(authService.getAuthStatus(request));
  }
}