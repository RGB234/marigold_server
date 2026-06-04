package com.sns.marigold.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.service.RecentAuthService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.config.SwaggerConfig;
import com.sns.marigold.global.dto.ApiResult;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.response.UserSecurityInfoDto;
import com.sns.marigold.user.dto.update.EmailPasswordRegisterDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.service.UserService;

import io.hypersistence.tsid.TSID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "User", description = "사용자 계정과 프로필 API")
@RestController
@RequestMapping(UrlConstants.USER_BASE)
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final RecentAuthService recentAuthService;

  @Operation(
      summary = "Email/Password 등록",
      description = "소셜 로그인으로 가입한 사용자가 이메일/비밀번호 로그인 수단을 추가합니다. 최근 인증이 필요합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "등록 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "최근 인증 필요"),
    @ApiResponse(responseCode = "409", description = "이미 등록된 이메일/비밀번호 정보 또는 중복 이메일")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/credentials")
  public ResponseEntity<ApiResult<Void>> registerEmailAndPassword(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Valid
          @RequestBody(description = "등록할 이메일과 비밀번호", required = true)
          @org.springframework.web.bind.annotation.RequestBody
          EmailPasswordRegisterDto dto,
      @Parameter(hidden = true) HttpServletRequest request) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw com.sns.marigold.auth.exception.AuthException.forUnauthorized();
    }
    recentAuthService.validate(request, userId);
    userService.registerEmailAndPassword(userId, dto);
    return ResponseEntity.ok(
        ApiResult.success(HttpStatus.OK, "Credentials registered successfully"));
  }

  @Operation(
      summary = "내 프로필 수정",
      description = "인증된 사용자의 닉네임과 프로필 이미지를 수정합니다. multipart/form-data 요청입니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "수정 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 또는 이미지 파일 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "409", description = "중복 닉네임")
  })
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<Void>> update(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "수정할 프로필 정보") @ModelAttribute @Valid UserUpdateDto dto) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw com.sns.marigold.auth.exception.AuthException.forUnauthorized();
    }
    userService.updateUser(userId, dto);
    return ResponseEntity.ok(ApiResult.success(HttpStatus.OK, "User updated successfully"));
  }

  // ** get **

  @Operation(
      summary = "내 보안 정보 조회",
      description = "인증된 사용자의 이메일, 로컬 로그인, OAuth2 연동 상태를 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "404", description = "사용자 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/security")
  public ResponseEntity<ApiResult<UserSecurityInfoDto>> getSecurityInfo(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw com.sns.marigold.auth.exception.AuthException.forUnauthorized();
    }
    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK,
            "User security info fetched successfully",
            userService.getSecurityInfo(userId)));
  }

  // 검색
  @Operation(summary = "닉네임으로 사용자 검색", description = "닉네임 키워드로 사용자 프로필 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "검색어 누락")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("/search")
  public ApiResult<List<UserInfoDto>> getPersonByNickname(
      @Parameter(description = "검색할 닉네임 키워드", required = true) @RequestParam("query")
          String nickname) {
    return ApiResult.success(
        HttpStatus.OK, "User search fetched successfully", userService.getUserByNickname(nickname));
  }

  @Operation(summary = "사용자 프로필 조회", description = "사용자 ID로 공개 프로필을 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "사용자 없음")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("/profile/{userId}")
  public ResponseEntity<ApiResult<UserInfoDto>> getPersonProfile(
      @Parameter(description = "TSID 형식 사용자 ID", required = true) @PathVariable("userId")
          String userId) {
    long uid;
    try {
      uid = TSID.from(userId).toLong();
    } catch (IllegalArgumentException e) {
      throw UserException.forUserNotFound();
    }

    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK, "User profile fetched successfully", userService.getUserById(uid)));
  }

  // ** delete **

  @Operation(
      summary = "회원 탈퇴",
      description = "인증된 사용자를 탈퇴 처리합니다. 최근 인증이 필요합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "탈퇴 성공"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "최근 인증 필요 또는 권한 없음"),
    @ApiResponse(responseCode = "404", description = "사용자 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/delete")
  public ResponseEntity<ApiResult<Void>> deleteUser(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(hidden = true) HttpServletRequest request) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw com.sns.marigold.auth.exception.AuthException.forAccessDenied();
    }
    recentAuthService.validate(request, userId);
    userService.deleteUser(principal.getUserId());
    return ResponseEntity.ok(ApiResult.success(HttpStatus.OK, "User deleted successfully"));
  }
}
