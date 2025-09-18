package com.sns.marigold.user.service;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.UserResponseDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceFacade {

  private final InstitutionUserService institutionUserService;
  private final PersonalUserService personalUserService;

  private final UserRepository userRepository;

  public UserResponseDto getUser(Long uid) {
    User user = userRepository.findById(uid).orElseThrow(() ->
      new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")
    );

    if (user.getRole() == Role.ROLE_PERSON) {
      return UserResponseDto.fromPersonalUser(personalUserService.findById(uid));
    } else if (user.getRole() == Role.ROLE_INSTITUTION) {
      return UserResponseDto.fromInstitutionUser(institutionUserService.findById(uid));
    } else { // ROLE_ADMIN
      throw new EntityNotFoundException("해당 사용자를 찾을 수 없습니다");
    }
  }
}
