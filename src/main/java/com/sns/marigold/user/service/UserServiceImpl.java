package com.sns.marigold.user.service;

import com.sns.marigold.auth.RandomUsernameGenerator;
import com.sns.marigold.user.dto.UserCreateDTO;
import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserUpdateDTO;
import com.sns.marigold.user.entity.UserEntity;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final RandomUsernameGenerator randomUsernameGenerator;

  @Transactional
  @Override
  public UserProfileDTO create(UserCreateDTO userCreateDTO) {
    int maxAttempts = 3;

    for (int i = 0; i < maxAttempts; i++) {
      String username = randomUsernameGenerator.generate();
      UserEntity user = userCreateDTO.toUserEntity();

      user.setUsername(username);

      try {
        UserEntity savedUser = userRepository.save(user);
        return UserProfileDTO.fromUserEntity(savedUser);
      } catch (DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException : {}", e.getMessage());
        log.error("Retrying... ( {} / {} )", i + 1, maxAttempts);
      }
    }
    throw new IllegalStateException("계정 생성 실패. 잠시 후 다시 시도해주십시오.");
  }

  @Transactional
  @Override
  public UserProfileDTO update(Long id, UserUpdateDTO userUpdateDTO) {
    UserEntity userEntity =
      userRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

    userEntity.updateFrom(userUpdateDTO);
    return UserProfileDTO.fromUserEntity(userEntity);
  }

  @Override
  public UserProfileDTO get(String username) {
    UserEntity userEntity =
      userRepository
        .findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));

    return UserProfileDTO.fromUserEntity(userEntity);
  }

  @Override
  public boolean softDelete(Long id) {
    return false;
  }

  @Override
  public void hardDelete(Long id) {
    userRepository.deleteById(id);
  }

  @Override
  public boolean restore(Long id) {
    return false;
  }
}
