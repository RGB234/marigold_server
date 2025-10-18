package com.sns.marigold.user.service;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.AdminUserResponseDto;
import com.sns.marigold.user.dto.UserResponseDto;
import com.sns.marigold.user.entity.AdminUser;
import com.sns.marigold.user.repository.AdminUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService implements UserService {
  final AdminUserRepository adminUserRepository;

  @Override
  public UserResponseDto loadUserById(UUID id) {
    AdminUser user = adminUserRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

    return new UserResponseDto(null, null, AdminUserResponseDto.fromUser(user));
  }

  @Override
  public Role getRole() {
    return Role.ROLE_ADMIN;
  }
}
