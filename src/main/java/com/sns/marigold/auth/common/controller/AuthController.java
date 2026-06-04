package com.sns.marigold.auth.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.auth.common.csrf.CsrfTokenService;
import com.sns.marigold.auth.common.dto.LocalLoginDto;
import com.sns.marigold.auth.common.dto.LoginResponseDto;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.service.AuthService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.config.SwaggerConfig;
import com.sns.marigold.global.dto.ApiResult;
import com.sns.marigold.user.dto.create.LocalSignupDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// OAuth2 로그인은 Spring security가 처리
// SecurityConfig 및 관련 코드 참조
// OAuth2 인증 엔드포인드 -> /oauth2/authorization/{registrationId} (기본값)
// /oauth2/authorization/kakao
// /oauth2/authorization/naver

@Tag(name = "Auth", description = "회원가입, 로그인, 인증 상태 API")
@RestController
@RequestMapping(UrlConstants.AUTH_BASE)
@Slf4j
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final CsrfTokenService csrfTokenService;

  @Operation(summary = "로컬 회원가입", description = "이메일, 비밀번호, 닉네임으로 로컬 계정을 생성합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "회원가입 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 오류"),
    @ApiResponse(responseCode = "409", description = "중복 이메일 또는 닉네임")
  })
  @PreAuthorize("permitAll()")
  @PostMapping("/signup")
  public ResponseEntity<ApiResult<Void>> localSignup(@Valid @RequestBody LocalSignupDto dto) {
    authService.localSignup(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResult.success(HttpStatus.CREATED, "local signup successfully", null));
  }

  @Operation(
      summary = "로컬 로그인",
      description = "이메일/비밀번호로 로그인하고 accessToken을 응답 body로, refresh token과 CSRF token을 쿠키로 발급합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공"),
    @ApiResponse(responseCode = "400", description = "인증 정보 오류"),
    @ApiResponse(responseCode = "403", description = "탈퇴, 정지 또는 휴면 사용자")
  })
  @PreAuthorize("permitAll()")
  @PostMapping("/login")
  public ResponseEntity<ApiResult<LoginResponseDto>> localLogin(
      @Valid @RequestBody LocalLoginDto dto,
      @Parameter(hidden = true) HttpServletResponse response) {
    LoginResponseDto loginResponse = authService.localLogin(dto, response);
    csrfTokenService.issue(response);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "local login successfully", loginResponse));
  }

  /*
   * HttpOnly Cookie를 사용하여 인증 상태 관리
   * 프론트엔드 UI 업데이트를 위한 최소한의 인증 정보 전달 > 로그인 유무 및 권한
   * 헤더에 토큰이 있을 경우 JwtAuthenticationFilter에서 Authentication 객체를 생성하여 SecurityContext에 저장
   */
  @Operation(
      summary = "인증 상태 조회",
      description = "현재 accessToken 인증 상태와 refresh token 쿠키 존재 여부를 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "401", description = "accessToken 만료 또는 오류")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("/status")
  public ResponseEntity<ApiResult<UserAuthStatusDto>> getAuthStatus(
      @Parameter(hidden = true) Authentication authentication,
      @Parameter(hidden = true) HttpServletRequest request,
      @Parameter(hidden = true) HttpServletResponse response) {
    UserAuthStatusDto authStatus = authService.getAuthStatus(authentication, request);
    if (authStatus.isRefreshTokenPresent()) {
      csrfTokenService.issue(response);
    }

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "get auth status successfully", authStatus));
  }

  @Operation(
      summary = "accessToken 재발급",
      description = "refresh token 쿠키로 새 accessToken과 refresh token을 발급합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "재발급 성공"),
    @ApiResponse(responseCode = "401", description = "refresh token 누락, 만료 또는 오류"),
    @ApiResponse(responseCode = "403", description = "CSRF token 누락 또는 불일치")
  })
  @PreAuthorize("permitAll()")
  @PostMapping("/refresh")
  public ResponseEntity<ApiResult<LoginResponseDto>> refresh(
      @Parameter(hidden = true) HttpServletRequest request,
      @Parameter(hidden = true) HttpServletResponse response) {
    LoginResponseDto loginResponse = authService.reissue(request, response);
    csrfTokenService.issue(response);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "token refreshed successfully", loginResponse));
  }
}
