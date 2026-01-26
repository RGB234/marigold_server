package com.sns.marigold.user.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.service.UserService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;

  // ** create **

  @PostMapping("/create")
  public ResponseEntity<?> create(@RequestBody @Valid UserCreateDto dto) {
    UUID userId = userService.createUser(dto);
    return ResponseEntity.ok(Map.of(
        "userId", userId.toString(),
        "message", "User created successfully"));
  }

  // ** update **
  @PatchMapping(value="/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> update(
      @AuthenticationPrincipal CustomPrincipal principal, @ModelAttribute @Valid UserUpdateDto dto) {
    UUID userId = UUID.fromString(principal.getName());
    userService.updateUser(userId, dto);
    return ResponseEntity.ok(null);
  }

  // ** get **

  // 검색
  @GetMapping("/search")
  public List<UserInfoDto> getPersonByNickname(@RequestParam("query") String nickname) {
    return userService.getUserByNickname(nickname);
  }

  // 본인(현재 세션) 프로필 조회
  // @GetMapping("/profile")
  // public ResponseEntity<UserInfoDto> getCurrentProfile(
  //     @AuthenticationPrincipal CustomPrincipal principal) {
  //   UUID userId = UUID.fromString(principal.getName());
  //   return ResponseEntity.ok(userService.getUserById(userId));
  // }

  @GetMapping("/profile/{userId}")
  public ResponseEntity<UserInfoDto> getPersonProfile(@PathVariable("userId") UUID userId) {
    return ResponseEntity.ok(userService.getUserById(userId));
  }

  // ** delete **

  @DeleteMapping("/delete")
  public ResponseEntity<String> deletePerson(@AuthenticationPrincipal CustomPrincipal principal) {
    UUID userId = UUID.fromString(principal.getName());
    userService.deleteUser(userId);
    return ResponseEntity.ok().body("User deleted successfully");
  }
}
