package com.sns.marigold.user.controller;

import com.sns.marigold.user.dto.UserCreateDTO;
import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserUpdateDTO;
import com.sns.marigold.user.service.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @Controller("/user")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserServiceImpl userService;

  @PostMapping("/create")
  public UserProfileDTO create(@RequestBody @Valid UserCreateDTO userCreateDTO) {
    return userService.create(userCreateDTO);
  }

  @GetMapping("/{nickname}")
  public UserProfileDTO get(@PathVariable("nickname") String nickname) {
    return userService.get(nickname);
  }

  @PatchMapping("update/{id}")
  public UserProfileDTO update(
      @PathVariable Long id, @RequestBody @Valid UserUpdateDTO userUpdateDTO) {
    return userService.update(id, userUpdateDTO);
  }

  @DeleteMapping("/hard-delete/{id}")
  public ResponseEntity<String> hardDelete(@PathVariable("id") Long id) {
    userService.hardDelete(id);
    return ResponseEntity.ok().body("User deleted successfully");
  }

  //    @DeleteMapping("/soft-delete")
  //    public String softDelete(){
  //
  //    };
}
