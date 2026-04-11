package com.sns.marigold.user.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.dto.ApiResponse;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.service.UserService;
import io.hypersistence.tsid.TSID;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlConstants.USER_BASE)
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;

  // ** create **

  // @PreAuthorize("permitAll()")
  // @PostMapping("")
  // public ResponseEntity<ApiResponse<Long>> create(@RequestBody @Valid OAuth2SignupDto dto) {
  //   Long userId = authService.oauth2Signup(dto);
  //   return ResponseEntity.ok(
  //       ApiResponse.success(HttpStatus.CREATED, "User created successfully", userId));
  // }

  // ** update **
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomPrincipal principal,
      @ModelAttribute @Valid UserUpdateDto dto) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw com.sns.marigold.auth.exception.AuthException.forUnauthorized();
    }
    userService.updateUser(userId, dto);
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User updated successfully"));
  }

  // ** get **

  // 검색
  @PreAuthorize("permitAll()")
  @GetMapping("/search")
  public ApiResponse<List<UserInfoDto>> getPersonByNickname(
      @RequestParam("query") String nickname) {
    return ApiResponse.success(
        HttpStatus.OK, "User search fetched successfully", userService.getUserByNickname(nickname));
  }

  @PreAuthorize("permitAll()")
  @GetMapping("/profile/{userId}")
  public ResponseEntity<ApiResponse<UserInfoDto>> getPersonProfile(
      @PathVariable("userId") String userId) {
    long uid;
    try {
      uid = TSID.from(userId).toLong();
    } catch (IllegalArgumentException e) {
      throw UserException.forUserNotFound();
    }

    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK, "User profile fetched successfully", userService.getUserById(uid)));
  }

  // ** delete **

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/delete")
  public ResponseEntity<ApiResponse<Void>> deletePerson(
      @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw com.sns.marigold.auth.exception.AuthException.forUnauthorized();
    }
    userService.deleteUser(userId);
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "User deleted successfully"));
  }
}
