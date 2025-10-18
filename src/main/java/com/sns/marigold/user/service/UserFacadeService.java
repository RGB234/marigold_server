package com.sns.marigold.user.service;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.UserResponseDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserFacadeService {
  private final UserRepository userRepository;
  private final Map<Role, UserService> userServiceMap;

  public UserFacadeService(UserRepository userRepository, List<UserService> userServices) {
    this.userRepository = userRepository;
    this.userServiceMap = userServices.stream()
        .collect(Collectors.toMap(UserService::getRole, Function.identity()));
  }

  public User findById(UUID id) {
    return userRepository.findById(id).orElseThrow(
        () -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")
    );
  }

  public UserResponseDto loadUserById(UUID id) {
    User user = userRepository.findById(id).orElseThrow(
        () -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")
    );

    Role role = user.getRole();
    UserService userService = userServiceMap.get(role);
    if (userService == null) {
      throw new IllegalArgumentException("지원되지 않는 사용자 역할입니다: " + role);
    }

    return userService.loadUserById(id);
  }
}
