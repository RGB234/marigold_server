package com.sns.marigold.user.service;

import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;

  public User findById(UUID id) {
    return userRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
  }
}
