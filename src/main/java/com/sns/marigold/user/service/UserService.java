package com.sns.marigold.user.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.service.StorageService;
import com.sns.marigold.storage.service.StorageUploadService;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.response.UserSecurityInfoDto;
import com.sns.marigold.user.dto.update.EmailPasswordRegisterDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.entity.UserImage;
import com.sns.marigold.user.event.UserDeletedEvent;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final StorageService storageService;
  private final StorageUploadService storageUploadService;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final PasswordEncoder passwordEncoder;
  private final AuditLogger auditLogger;

  @Transactional(readOnly = true)
  public boolean existsByProviderInfoAndProviderId(ProviderInfo providerInfo, String providerId) {
    return userRepository.existsByProviderInfoAndProviderId(providerInfo, providerId);
  }

  @Transactional(readOnly = true)
  public Optional<User> findEntityByProviderInfoAndProviderId(
      ProviderInfo providerInfo, String providerId) {
    return userRepository.findByProviderInfoAndProviderId(providerInfo, providerId);
  }

  @Transactional(readOnly = true)
  public User findEntityById(Long uid) {
    Long userId = Objects.requireNonNull(uid, "사용자 ID가 비었습니다.");
    return userRepository.findById(userId).orElseThrow(() -> UserException.forUserNotFound());
  }

  @Transactional(readOnly = true)
  public UserInfoDto getUserById(Long uid) throws UsernameNotFoundException {
    User user = findEntityById(uid);
    UserInfoDto dto = UserInfoDto.from(user);
    if (dto.getImageUrl() != null) {
      dto.setImageUrl(storageService.getViewUrlOrNull(dto.getImageUrl()));
    }
    return dto;
  }

  @Transactional(readOnly = true)
  public UserSecurityInfoDto getSecurityInfo(Long uid) {
    return UserSecurityInfoDto.from(findEntityById(uid));
  }

  @Transactional(readOnly = true)
  public List<UserInfoDto> getUserByNickname(String nickname) {
    List<User> users = userRepository.findPersonalUsersByNickname(nickname);
    return users.stream()
        .map(
            user -> {
              UserInfoDto dto = UserInfoDto.from(user);
              if (dto.getImageUrl() != null) {
                dto.setImageUrl(storageService.getViewUrlOrNull(dto.getImageUrl()));
              }
              return dto;
            })
        .toList();
  }

  @Transactional
  public void linkOAuth2(Long uid, ProviderInfo providerInfo, String providerId)
      throws UserException {
    User user = findEntityById(uid);
    if (user.hasOAuth2Link()) {
      throw UserException.forUserOAuth2AlreadyLinked();
    }
    if (userRepository.existsByProviderInfoAndProviderId(providerInfo, providerId)) {
      throw UserException.forUserOAuth2AccountAlreadyInUse();
    }
    user.linkOAuth2(providerInfo, providerId);
    auditLogger.info("event=oauth2_linked userId={} provider={}", user.getId(), providerInfo);
  }

  @Transactional
  public void registerEmailAndPassword(Long uid, EmailPasswordRegisterDto dto) {
    User user = findEntityById(uid);
    if (user.hasLocalCredentials()) {
      throw UserException.forUserLocalCredentialsAlreadyExists();
    }
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw UserException.forUserAlreadyExists();
    }
    user.addEmailAndPassword(dto.getEmail(), passwordEncoder.encode(dto.getPassword()));
    auditLogger.info("event=local_credentials_registered userId={}", user.getId());
  }

  // 업로드 확정 동안 DB 커넥션을 점유하지 않도록 트랜잭션은 TransactionTemplate 구간으로 한정
  public void updateUser(Long uid, UserUpdateDto dto) {
    // 1. 이미지 확정. 이후 저장이 실패해도 확정된 파일은 고아 정리가 지운다
    final FileUploadDto newImageUploadDto =
        !dto.isRemoveImage() && dto.getImageUploadToken() != null
            ? storageUploadService
                .confirm(uid, UploadPurpose.USER_PROFILE_IMAGE, List.of(dto.getImageUploadToken()))
                .get(0)
            : null;

    // 2. DB 트랜잭션 (데이터 변경)
    transactionTemplate.executeWithoutResult(
        status -> {
          User user = findEntityById(uid);
          UserImage previousImage = user.getImage();
          String fileToDelete = null;

          // 사용자가 기본 프로필 사진 사용을 선택
          if (dto.isRemoveImage()) {
            fileToDelete = previousImage != null ? previousImage.getStoredFileName() : null;
            user.update(dto.getNickname(), null); // DB에서 이미지 삭제 (CASCADE)
          } else if (newImageUploadDto != null) {
            // 새 이미지로 교체
            fileToDelete = previousImage != null ? previousImage.getStoredFileName() : null;

            UserImage newImage =
                UserImage.builder()
                    .storedFileName(newImageUploadDto.getStoredFileName())
                    .originalFileName(newImageUploadDto.getOriginalFileName())
                    .build();
            user.update(dto.getNickname(), newImage);
          } else {
            // 닉네임만 변경 (이미지는 유지)
            user.update(dto.getNickname());
          }

          // 3. 트랜잭션 성공 시: 삭제해야 할 기존 이미지(프로필 제거 또는 교체 시)가 있다면 이벤트 발행
          if (fileToDelete != null) {
            eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(List.of(fileToDelete)));
          }
        });
  }

  // soft delete And PII scrubbing
  @Transactional
  public void deleteUser(@NonNull Long id) {
    User user = findEntityById(id);
    String profileImage = user.getImage() != null ? user.getImage().getStoredFileName() : null;

    user.softDelete();

    // 게시글·채팅방 정리는 각 도메인 리스너가 같은 트랜잭션 안에서 동기로 수행
    eventPublisher.publishEvent(new UserDeletedEvent(id));

    auditLogger.info("event=user_deleted userId={}", id);
    // 트랜잭션 종료 시 스토리지에서 이미지 삭제
    if (profileImage != null) {
      eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(List.of(profileImage)));
    }
  }
}
