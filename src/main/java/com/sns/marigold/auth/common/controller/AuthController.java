package com.sns.marigold.auth.common.controller;

import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
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
  public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response, Authentication authentication) {
    authService.logout(response, authentication);
    return ResponseEntity.ok().body(Map.of("message", "logout success"));
  }

  /*
    HttpOnly Cookie를 사용하여 인증 상태 관리
    프론트엔드 UI 업데이트를 위한 최소한의 인증 정보 전달 > 로그인 유무 및 권한
    헤더에 토큰이 있을 경우 JwtAuthenticationFilter에서 Authentication 객체 생성 후 SecurityContext에 저장
    없을 경우 그냥 JwtAuthenticationFilter 통과함
   */
  @GetMapping("/status")
  public ResponseEntity<UserAuthStatusDto> getAuthStatus(Authentication authentication) {
    return ResponseEntity.ok().body(authService.getAuthStatus(authentication));
  }
}