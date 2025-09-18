package com.sns.marigold.user.service;

import com.sns.marigold.user.dto.InstitutionUserCreateDto;
import com.sns.marigold.user.dto.InstitutionUserResponseDto;
import com.sns.marigold.user.dto.InstitutionUserUpdateDto;
import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.repository.InstitutionUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InstitutionUserService {

  private final InstitutionUserRepository institutionUserRepository;
  private final PasswordEncoder passwordEncoder;

  public InstitutionUser findById(Long id) {
    return institutionUserRepository
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
  }

  public InstitutionUser findByUsername(String username) {
    return institutionUserRepository
      .findByUsername(username)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
  }

  public InstitutionUser findByEmail(String email) {
    return institutionUserRepository.findByEmail(email)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
  }

  // ** create **

  @Transactional
  public InstitutionUserResponseDto create(InstitutionUserCreateDto dto) {
    String encodedPassword = passwordEncoder.encode(dto.getPassword());

    InstitutionUser user = InstitutionUser.builder()
      .username(dto.getUsername())
      .email(dto.getEmail())
      .password(encodedPassword)
      .contactPerson(dto.getContactPerson())
      .contactPhone(dto.getContactPhone())
      .registrationNumber(dto.getRegistrationNumber())
      .address(dto.getAddress())
      .build();

    institutionUserRepository.save(user);
    return InstitutionUserResponseDto.fromUser(user);
  }

  // ** update **

  @Transactional
  public InstitutionUserResponseDto update(Long id, InstitutionUserUpdateDto dto) {
    int updatedRows = institutionUserRepository.update(id, dto.getUsername(), dto.getEmail(),
      dto.getContactPerson(),
      dto.getContactPhone(), dto.getRegistrationNumber(), dto.getAddress());

    if (updatedRows == 0) {
      throw new EntityNotFoundException("해당 사용자를 찾을 수 없습니다");
    }

    InstitutionUser user = institutionUserRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

    return InstitutionUserResponseDto.fromUser(user);
  }

  // ** Get **

  public InstitutionUserResponseDto get(Long id) {
    InstitutionUser user = findById(id);
    return InstitutionUserResponseDto.fromUser(user);
  }

  public InstitutionUserResponseDto getByUsername(String username) {
    InstitutionUser user = findByUsername(username);
    return InstitutionUserResponseDto.fromUser(user);
  }

  // ** delete **

  public void delete(Long id) {
    institutionUserRepository.deleteById(id);
  }
}
