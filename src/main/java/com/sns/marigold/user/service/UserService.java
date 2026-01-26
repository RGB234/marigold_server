package com.sns.marigold.user.service;

import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.auth.oauth2.RandomUsernameGenerator;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.dto.ImageUploadDto;
import com.sns.marigold.global.error.exception.UserAlreadyExistsException;
import com.sns.marigold.global.service.S3Service;
import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.entity.UserImage;
import com.sns.marigold.user.event.UserDeletedEvent;
import com.sns.marigold.user.repository.UserRepository;

import io.micrometer.common.lang.NonNull;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor

public class UserService {

  private final UserRepository userRepository;
  private final AdoptionInfoRepository adoptionInfoRepository;
  private final RandomUsernameGenerator randomUsernameGenerator;
  private final S3Service s3Service;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public boolean existsByProviderInfoAndProviderId(@NonNull ProviderInfo providerInfo, @NonNull String providerId) {
    return userRepository.existsByProviderInfoAndProviderId(providerInfo, providerId);
  }

  @Transactional(readOnly = true)
  public Optional<User> findEntityByProviderInfoAndProviderId(@NonNull ProviderInfo providerInfo,
      @NonNull String providerId) {
    return userRepository.findByProviderInfoAndProviderId(providerInfo, providerId);
  }

  @Transactional(readOnly = true)
  public User findEntityById(Long uid) {
    Long userId = Objects.requireNonNull(uid, "사용자 ID가 비었습니다.");
    return userRepository.findById(userId)
        .orElseThrow(() -> (new EntityNotFoundException("해당 사용자를 찾을 수 없습니다")));
  }

  @Transactional(readOnly = true)
  public UserInfoDto getUserById(Long uid) throws UsernameNotFoundException {
    User user = findEntityById(uid);
    return UserInfoDto.from(user);
  }

  @Transactional(readOnly = true)
  public List<UserInfoDto> getUserByNickname(String nickname) {
    List<User> users = userRepository.findPersonalUsersByNickname(nickname);
    return users.stream()
        .map(UserInfoDto::from)
        .toList();
  }

  @Transactional
  public Long createUser(UserCreateDto dto) {
    Objects.requireNonNull(dto, "UserCreateDto는 null일 수 없습니다.");

    if (existsByProviderInfoAndProviderId(dto.getProviderInfo(), dto.getProviderId())) {
      throw new UserAlreadyExistsException();
    }
    ;

    // nickname 자동 생성
    String generatedNickname = generateUniqueNickname();

    // User 엔티티 생성
    User user = User.builder()
        .providerInfo(dto.getProviderInfo())
        .providerId(dto.getProviderId())
        .nickname(generatedNickname)
        .role(dto.getRole())
        .build();

    userRepository.save(user);

    // 저장
    return user.getId();
  }

  /**
   * 고유한 nickname 생성 (중복 체크 포함)
   */
  private String generateUniqueNickname() {
    int maxAttempts = 10; // 최대 시도 횟수
    String nickname;

    for (int i = 0; i < maxAttempts; i++) {
      nickname = randomUsernameGenerator.generate();

      // 중복 체크
      if (!userRepository.existsByNickname(nickname)) {
        return nickname;
      }

      log.warn("Nickname 중복 발생, 재생성 시도: {}", nickname);
    }

    // 최대 시도 횟수 초과 시 예외 발생
    throw new IllegalStateException("고유한 nickname 생성에 실패했습니다. 최대 시도 횟수를 초과했습니다.");
  }

  // 새 이미지 업로드 -> DB 저장 -> (성공시) 이전 이미지 삭제 / (실패시) 롤백 - 새 이미지 삭제

  public void updateUser(Long uid,
      UserUpdateDto dto) {
    // 새 이미지 업로드
    ImageUploadDto newImageUploadDto = null;
    if (dto.getImage() != null && !dto.getImage().isEmpty()) {
      newImageUploadDto = s3Service.uploadFile(dto.getImage());
    }
    final AtomicReference<UserImage> previousImage = new AtomicReference<>();
    final ImageUploadDto finalNewImageUploadDto = newImageUploadDto;
    try {
      // DB 저장
      transactionTemplate.executeWithoutResult(status -> {
        User user = findEntityById(uid);
        // 업로드 이미지가 있다면
        if (finalNewImageUploadDto != null) {
          // 기존 이미지가 있었다면 저장
          if (user.getImage() != null) {
            previousImage.set(user.getImage());
          }

          UserImage newImage = UserImage.builder().imageUrl(finalNewImageUploadDto.getImageUrl())
              .storeFileName(finalNewImageUploadDto.getStoreFileName())
              .originalFileName(finalNewImageUploadDto.getOriginalFileName()).build();

          user.update(dto.getNickname(), newImage);
        } else {
          if (dto.isRemoveImage()) { // 사용자가 기본 프로필 사진 사용을 선택
            user.update(dto.getNickname(), null); // DB에서 이미지 삭제. null일 경우 프론트에서 기본 프로필 사진 사용하도록 처리

          } else {
            user.update(dto.getNickname(), user.getImage());
          }
        }
      });
      if (previousImage.get() != null) {
        // 이전 이미지 삭제
        s3Service.deleteFile(previousImage.get().getStoreFileName());
      }
    } catch (Exception e) {
      // 새 이미지 저장 취소
      if (newImageUploadDto != null) {
        log.warn("DB update failed. Deleting new S3 file: {}", newImageUploadDto.getStoreFileName());
        s3Service.deleteFile(newImageUploadDto.getStoreFileName());
      }
      throw e;
    }
  }

  // soft delete And PII scrubbing
  @Transactional
  public void deleteUser(Long uid) {
    User user = findEntityById(uid);
    List<String> imageUrls = new ArrayList<>();
    // 사용자 프로필 이미지 url 주소 백업
    if (user.getImage() != null) {
      imageUrls.add(user.getImage().getStoreFileName());
    }

    // 작성자 게시글 이미지 스토리지 저장 파일명 조회 및 백업
    adoptionInfoRepository.findStoreFileNamesByWriterAndCompletedIsFalse(uid).forEach(imageUrls::add);
    // 작성자 게시글의 이미지부터 삭제
    // 여기서 clearAutomatically=true 발동 -> 영속성 컨텍스트 초기화됨
    adoptionInfoRepository.deleteImagesByWriterAndCompletedIsFalse(uid);
    // 작성자 게시글 삭제
    // 여기서 clearAutomatically=true 발동 -> 영속성 컨텍스트 초기화됨
    adoptionInfoRepository.deleteByWriterAndCompletedIsFalse(uid);

    // soft delete
    user.softDelete();
    // 주의: 위에서 컨텍스트가 비워졌기 때문에 user는 현재 '준영속' 상태입니다.
    // 하지만 save()를 호출하면 JPA가 다시 'merge(병합)'를 시도하므로 정상 동작합니다.
    userRepository.save(user);
    // 트랜잭션 종료 시 스토리지 상 이미지 삭제
    if (!imageUrls.isEmpty()) {
      eventPublisher.publishEvent(new UserDeletedEvent(imageUrls));
    }
  }
}
