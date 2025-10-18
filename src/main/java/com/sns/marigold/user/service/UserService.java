package com.sns.marigold.user.service;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.UserResponseDto;

import java.util.UUID;

public interface UserService {
  public UserResponseDto loadUserById(UUID id);

  public Role getRole();
}
