package com.sns.marigold.user.service;

import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

//  public PersonalUser findByProviderInfoAndProviderId(ProviderInfo providerInfo,
//                                                      String providerId) {
//    return personalUserRepository
//        .findByProviderInfoAndProviderId(providerInfo,
//            providerId)
//        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
//  }

  @Override
  public User findEntityById(UUID uid) {
    UUID userId = Objects.requireNonNull(uid, "사용자 ID가 비었습니다.");
    return userRepository.findById(userId)
        .orElseThrow(() -> (new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")));
  }

  @Override
  public UserInfoDto getUserById(UUID uid) throws UsernameNotFoundException {
    UUID userId = Objects.requireNonNull(uid, "사용자 ID가 비었습니다.");
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
    return UserInfoDto.from(user);
  }

  @Override
  public List<UserInfoDto> getUserByNickname(String nickname) {
    List<User> users = userRepository.findPersonalUsersByNickname(nickname);
    return users.stream()
        .map(UserInfoDto::from)
        .toList();
  }

  @Transactional
  @Override
  public UUID createUser(UserCreateDto dto) {
    User user = Objects.requireNonNull(dto.toEntity(),
        "개인 회원 생성에 실패했습니다.");
    User savedUser = userRepository.save(user);
    UUID savedId = savedUser.getId();
    if (savedId == null) {
      throw new IllegalStateException("저장된 개인 회원의 ID가 비어 있습니다.");
    }
    return savedId;
  }

  @Transactional
  @Override
  public void updateUser(UUID uid,
                         UserUpdateDto dto) {
    UUID userId = Objects.requireNonNull(uid, "사용자 ID가 비었습니다.");
    User user = userRepository.findById(userId)
        .orElseThrow(() ->
            new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
    user.applyUpdate(dto);
  }

  @Override
  public void deleteUser(UUID uid) {
    UUID userId = Objects.requireNonNull(uid, "사용자 ID가 비었습니다.");
    if (!userRepository.existsById(userId)) {
      throw new EntityNotFoundException("해당 사용자를 찾을 수 없습니다");
    }
    userRepository.deleteById(uid);
  }
}
