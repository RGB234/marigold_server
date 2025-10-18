package com.sns.marigold.user.service;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.*;
import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.repository.InstitutionUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InstitutionUserService implements UserService {

  private final InstitutionUserRepository institutionUserRepository;
  private final PasswordEncoder passwordEncoder;

  public InstitutionUser findById(UUID id) {
    return institutionUserRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
  }

  public InstitutionUser findByUsername(String username) {
    return institutionUserRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
  }

  public List<InstitutionUser> findByCompanyName(String companyName) {
    return institutionUserRepository.findByCompanyName(companyName);
  }

  // ** create **

  @Transactional
  public UUID create(InstitutionUserCreateDto dto) {
    String encodedPassword = passwordEncoder.encode(dto.getPassword());
    InstitutionUser user = InstitutionUser.builder()
        .username(dto.getUsername())
        .password(encodedPassword)
        .email(dto.getEmail())
        .companyName(dto.getCompanyName())
        .repName(dto.getRepName())
        .brn(dto.getBrn())
        .zipCode(dto.getZipCode())
        .address(dto.getAddress())
        .detailedAddress(dto.getDetailedAddress())
        .build();

    institutionUserRepository.save(user);
//    return InstitutionUserResponseDto.fromUser(user);
    return user.getId();
  }

  // ** update **

  @Transactional
  public InstitutionUserResponseDto update(UUID id, InstitutionUserUpdateDto dto) {
    InstitutionUser user = institutionUserRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

    user.update(dto);
    return InstitutionUserResponseDto.fromUser(user);
  }

  @Transactional
  public InstitutionUserResponseDto updateSecurityInfo(UUID id,
                                                       InstitutionUserSecurityUpdateDto dto) {
    InstitutionUser user = institutionUserRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

    user.updateSecurityInfo(dto, passwordEncoder);
    return InstitutionUserResponseDto.fromUser(user);
  }

  // ** Get **

  public List<InstitutionUserResponseDto> getByCompanyName(String companyName) {
    List<InstitutionUser> users = institutionUserRepository.findByCompanyName(companyName);
    return users.stream().map(InstitutionUserResponseDto::fromUser).toList();
  }

  public InstitutionUserResponseDto getById(UUID id) {
    InstitutionUser user = institutionUserRepository.findById(id).orElseThrow(
        () -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")
    );
    return InstitutionUserResponseDto.fromUser(user);
  }


  // ** delete **

  public void delete(UUID id) {
    if (!institutionUserRepository.existsById(id)) {
      throw new EntityNotFoundException("해당 사용자를 찾을 수 없습니다");
    }
    institutionUserRepository.deleteById(id);

  }

  @Override
  public UserResponseDto loadUserById(UUID id) {
    InstitutionUser user = institutionUserRepository.findById(id).orElseThrow(
        () -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")
    );
    return new UserResponseDto(null, InstitutionUserResponseDto.fromUser(user), null);
  }

  @Override
  public Role getRole() {
    return Role.ROLE_INSTITUTION;
  }


}
