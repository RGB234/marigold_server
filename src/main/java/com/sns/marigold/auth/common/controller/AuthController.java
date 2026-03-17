package com.sns.marigold.auth.common.controller;

import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.service.AuthService;
import com.sns.marigold.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


// OAuth2 로그인은 Spring security가 처리
// SecurityConfig 및 관련 코드 참조
// OAuth2 인증 엔드포인드 -> /oauth2/authorization/{registrationId} (기본값)
// /oauth2/authorization/kakao
// /oauth2/authorization/naver

@RestController
@RequestMapping(UrlConstants.AUTH_BASE)
@Slf4j
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /*
   * HttpOnly Cookie를 사용하여 인증 상태 관리
   * 프론트엔드 UI 업데이트를 위한 최소한의 인증 정보 전달 > 로그인 유무 및 권한
   * 헤더에 토큰이 있을 경우 JwtAuthenticationFilter에서 Authentication 객체를 생성하여 SecurityContext에 저장
   */
  @PreAuthorize("permitAll()")
  @GetMapping("/status")
  public ResponseEntity<ApiResponse<UserAuthStatusDto>> getAuthStatus(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK).body(
        ApiResponse.success(HttpStatus.OK, "get auth status successfully", authService.getAuthStatus(authentication)));
  }

  // @GetMapping("/reissue")
  // public ResponseEntity<Map<String, Object>> reissue(HttpServletRequest
  // request, HttpServletResponse response) {
  // authService.reissue(request, response);
  // return ResponseEntity.ok().body(Map.of("message", "reissue success"));
  // }
}