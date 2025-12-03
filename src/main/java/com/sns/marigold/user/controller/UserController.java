package com.sns.marigold.user.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.service.UserServiceImpl;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserServiceImpl personalUserService;

  // ** create **

  @PostMapping("/create")
  public ResponseEntity<?> create(@RequestBody @Valid UserCreateDto dto) {
    personalUserService.createUser(dto);
    return ResponseEntity.ok(null);
  }

  // ** update **
  @PatchMapping("settings")
  public ResponseEntity<?> update(
      @SessionAttribute("uid") UUID uid, @RequestBody @Valid UserUpdateDto dto
  ) {
    personalUserService.updateUser(uid, dto);
    return ResponseEntity.ok(null);
  }

  // ** get **

  // 검색
  @GetMapping("/search/{nickname}")
  public List<UserInfoDto> getPersonByNickname(@PathVariable("nickname") String nickname) {
    return personalUserService.getUserByNickname(nickname);
  }

  // 본인(현재 세션) 프로필 조회
  @GetMapping("/profile")
  public ResponseEntity<UserInfoDto> getCurrentProfile(
      @AuthenticationPrincipal CustomPrincipal principal
  ) {
    UUID userId = principal.getId();
    return ResponseEntity.ok(personalUserService.getUserById(userId));
  }

  @GetMapping("/profile/{userId}")
  public ResponseEntity<UserInfoDto> getPersonProfile(@PathVariable("userId") UUID userId) {
    return ResponseEntity.ok(personalUserService.getUserById(userId));
  }
  
  // ** delete **

  @DeleteMapping("/delete/person")
  public ResponseEntity<String> deletePerson(@SessionAttribute("uid") UUID uid) {
    personalUserService.deleteUser(uid);
    return ResponseEntity.ok().body("User deleted successfully");
  }
}
