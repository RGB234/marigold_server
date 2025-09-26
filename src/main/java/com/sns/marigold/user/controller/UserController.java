package com.sns.marigold.user.controller;

import com.sns.marigold.user.dto.InstitutionUserCreateDto;
import com.sns.marigold.user.dto.InstitutionUserResponseDto;
import com.sns.marigold.user.dto.InstitutionUserSecurityUpdateDto;
import com.sns.marigold.user.dto.InstitutionUserUpdateDto;
import com.sns.marigold.user.dto.PersonalUserCreateDto;
import com.sns.marigold.user.dto.PersonalUserResponseDto;
import com.sns.marigold.user.dto.PersonalUserUpdateDto;
import com.sns.marigold.user.service.InstitutionUserService;
import com.sns.marigold.user.service.PersonalUserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
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

  private final InstitutionUserService institutionUserService;
  private final PersonalUserService personalUserService;

  // ** create **

  @PostMapping("/create/institution")
  public ResponseEntity<?> create(@RequestBody @Valid InstitutionUserCreateDto dto,
    BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
    }
    UUID id = institutionUserService.create(dto);
    return ResponseEntity.ok(institutionUserService.getById(id));
  }

  @PostMapping("/create/person")
  public PersonalUserResponseDto create(@RequestBody @Valid PersonalUserCreateDto dto) {
    UUID id = personalUserService.create(dto);
    return personalUserService.getById(id);
  }

  // ** update **
  @RolesAllowed({"ROLE_INSTITUTION"})
  @PatchMapping("settings/institution")
  public InstitutionUserResponseDto update(
    @SessionAttribute("uid") UUID uid, @RequestBody @Valid InstitutionUserUpdateDto dto) {
    return institutionUserService.update(uid, dto);
  }

  @RolesAllowed({"ROLE_INSTITUTION"})
  @PatchMapping("settings/institution/security")
  public InstitutionUserResponseDto updateSecurityInfo(
    @SessionAttribute("uid") UUID uid, @RequestBody @Valid InstitutionUserSecurityUpdateDto dto) {
    return institutionUserService.updateSecurityInfo(uid, dto);
  }

  @RolesAllowed({"ROLE_PERSON"})
  @PatchMapping("settings/person")
  public PersonalUserResponseDto update(
    @SessionAttribute("uid") UUID uid, @RequestBody @Valid PersonalUserUpdateDto dto
  ) {
    return personalUserService.update(uid, dto);
  }

  // ** get **

  @GetMapping("/institution/{companyName}")
  public List<InstitutionUserResponseDto> getInstitutionByCompanyName(
    @PathVariable("companyName") String companyName) {
    return institutionUserService.getByCompanyName(companyName);
  }

  @GetMapping("/person/{username}")
  public PersonalUserResponseDto getPersonByUsername(@PathVariable("username") String username) {
    return personalUserService.getByUsername(username);
  }

  // ** delete **

  @RolesAllowed({"ROLE_INSTITUTION"})
  @DeleteMapping("/delete/institution")
  public ResponseEntity<String> deleteInstitution(@SessionAttribute("uid") UUID uid) {
    institutionUserService.delete(uid);
    return ResponseEntity.ok().body("User deleted successfully");
  }

  @RolesAllowed({"ROLE_PERSON"})
  @DeleteMapping("/delete/person")
  public ResponseEntity<String> deletePerson(@SessionAttribute("uid") UUID uid) {
    personalUserService.delete(uid);
    return ResponseEntity.ok().body("User deleted successfully");
  }
}
