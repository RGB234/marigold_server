package com.sns.marigold.user.service;

import java.util.ArrayList;
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

import com.sns.marigold.adoption.repository.AdoptionPostImageRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.response.UserSecurityInfoDto;
import com.sns.marigold.user.dto.update.EmailPasswordRegisterDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.entity.UserImage;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final AdoptionPostRepository adoptionPostRepository;
  private final AdoptionPostImageRepository adoptionPostImageRepository;
  private final S3Service s3Service;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final PasswordEncoder passwordEncoder;
  private final ChatService chatService;

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
      dto.setImageUrl(s3Service.getPresignedGetObject(dto.getImageUrl()));
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
                dto.setImageUrl(s3Service.getPresignedGetObject(dto.getImageUrl()));
              }
              return dto;
            })
        .toList();
  }

  @Transactional
  public void linkOAuth2(Long uid, ProviderInfo providerInfo, String providerId) {
    User user = findEntityById(uid);
    if (user.hasOAuth2Link()) {
      throw UserException.forUserOAuth2AlreadyLinked();
    }
    if (userRepository.existsByProviderInfoAndProviderId(providerInfo, providerId)) {
      throw UserException.forUserOAuth2AccountAlreadyInUse();
    }
    user.linkOAuth2(providerInfo, providerId);
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
  }

  @Transactional
  public void updateUser(Long uid, UserUpdateDto dto) {
    // 1. 이미지 업로드 (트랜잭션 밖에서 수행)
    ImageUploadDto uploadedImageDto = null;
    if (dto.getImage() != null && !dto.getImage().isEmpty()) {
      uploadedImageDto = s3Service.uploadFile(dto.getImage());
    }

    try {
      final ImageUploadDto newImageUploadDto = uploadedImageDto;

      // 2. DB 트랜잭션 (데이터 변경)
      transactionTemplate.executeWithoutResult(
          status -> {
            // 영속성 컨텍스트 내에서 엔티티 재조회 (필수)
            User user = findEntityById(uid);
            UserImage previousImage = user.getImage();
            String fileToDelete = null;

            // 사용자가 기본 프로필 사진 사용을 선택
            if (dto.isRemoveImage()) {
              fileToDelete = previousImage != null ? previousImage.getStoredFileName() : null;
              user.update(dto.getNickname(), null); // DB에서 이미지 삭제
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

    } catch (Exception e) {
      // 4. 실패 시 보상 트랜잭션: 새로 업로드한 S3 파일 삭제
      if (uploadedImageDto != null) {
        log.error("Update user failed. Deleting uploaded S3 file... error: {}", e.getMessage());
        try {
          s3Service.deleteUploadedImagesFromS3(List.of(uploadedImageDto));
        } catch (Exception s3Ex) {
          log.error("Failed to delete S3 image during rollback. File: {}", uploadedImageDto, s3Ex);
        }
      }

      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  // soft delete And PII scrubbing
  @Transactional
  public void deleteUser(@NonNull Long id) {
    User user = findEntityById(id);
    List<String> imageUrls = new ArrayList<>();

    if (user.getImage() != null) {
      imageUrls.add(user.getImage().getStoredFileName());
    }

    List<String> postImageFileNames = adoptionPostImageRepository.findStoredFileNamesByWriter(id);
    if (postImageFileNames != null) {
      postImageFileNames.stream().filter(Objects::nonNull).forEach(imageUrls::add);
    }

    adoptionPostRepository.setDeletedTimeByWriter(id);
    adoptionPostImageRepository.deleteImagesByWriter(id);

    // 참가 중인 채팅방 CLOSE (read-only)
    chatService.closeAllChatRoomsByUserId(id);

    user.softDelete();
    userRepository.save(user);
    // 트랜잭션 종료 시 스토리지에서 이미지 삭제
    if (!imageUrls.isEmpty()) {
      eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imageUrls));
    }
  }
}
