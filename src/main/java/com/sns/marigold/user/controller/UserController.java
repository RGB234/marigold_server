package com.sns.marigold.user.controller;

import com.sns.marigold.user.dto.InstitutionUserCreateDto;
import com.sns.marigold.user.dto.InstitutionUserResponseDto;
import com.sns.marigold.user.dto.InstitutionUserUpdateDto;
import com.sns.marigold.user.dto.PersonalUserCreateDto;
import com.sns.marigold.user.dto.PersonalUserResponseDto;
import com.sns.marigold.user.dto.PersonalUserUpdateDto;
import com.sns.marigold.user.service.InstitutionUserService;
import com.sns.marigold.user.service.PersonalUserService;
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

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final InstitutionUserService institutionUserService;
  private final PersonalUserService personalUserService;

  // ** create **

  @PostMapping("/create/institution")
  public InstitutionUserResponseDto create(@RequestBody @Valid InstitutionUserCreateDto dto) {
    return institutionUserService.create(dto);
  }

  @PostMapping("/create/person")
  public PersonalUserResponseDto create(@RequestBody @Valid PersonalUserCreateDto dto) {
    return personalUserService.create(dto);
  }

  // ** update **

  @PatchMapping("settings/institution/{id}")
  public InstitutionUserResponseDto update(
    @PathVariable Long id, @RequestBody @Valid InstitutionUserUpdateDto dto) {
    return institutionUserService.update(id, dto);
  }

  @PatchMapping("settings/person/{id}")
  public PersonalUserResponseDto updateUsername(
    @PathVariable Long id, @RequestBody @Valid PersonalUserUpdateDto dto
  ) {
    return personalUserService.update(id, dto);
  }

  // ** get **

  @GetMapping("/institution/{username}")
  public InstitutionUserResponseDto getInstitution(@PathVariable("username") String username) {
    return institutionUserService.getByUsername(username);
  }

  @GetMapping("/person/{username}")
  public PersonalUserResponseDto getPerson(@PathVariable("username") String username) {
    return personalUserService.getByUsername(username);
  }

  // ** delete **

  @DeleteMapping("/delete/institution/{id}")
  public ResponseEntity<String> deleteInstitution(@PathVariable("id") Long id) {
    institutionUserService.delete(id);
    return ResponseEntity.ok().body("User deleted successfully");
  }

  @DeleteMapping("/delete/person/{id}")
  public ResponseEntity<String> deletePerson(@PathVariable("id") Long id) {
    personalUserService.delete(id);
    return ResponseEntity.ok().body("User deleted successfully");
  }
}
