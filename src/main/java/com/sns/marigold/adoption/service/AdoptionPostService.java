package com.sns.marigold.adoption.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.adoption.dto.AdoptionCandidateDto;
import com.sns.marigold.adoption.dto.AdoptionPostCreateDto;
import com.sns.marigold.adoption.dto.AdoptionPostDetailDto;
import com.sns.marigold.adoption.dto.AdoptionPostDto;
import com.sns.marigold.adoption.dto.AdoptionPostSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionPostUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionAdopter;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.entity.AdoptionPostEditor;
import com.sns.marigold.adoption.entity.AdoptionPostImage;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.exception.AdoptionPostException;
import com.sns.marigold.adoption.repository.AdoptionAdopterRepository;
import com.sns.marigold.adoption.repository.AdoptionPostImageRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.adoption.specification.AdoptionPostSpecification;
import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.StorageFileService;
import com.sns.marigold.storage.service.StorageService;
import com.sns.marigold.storage.service.StorageUploadService;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionPostService {

  private final UserService userService;
  private final StorageService storageService;
  private final StorageUploadService storageUploadService;
  private final StorageFileService storageFileService;
  private final AdoptionCommentService adoptionCommentService;
  private final ChatService chatService;

  private final AdoptionPostRepository adoptionPostRepository;
  private final AdoptionPostImageRepository adoptionPostImageRepository;
  private final AdoptionAdopterRepository adoptionAdopterRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final RoomParticipantRepository participantRepository;

  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final AuditLogger auditLogger;

  @Transactional(readOnly = true)
  public AdoptionPost findEntityById(Long id) {
    return adoptionPostRepository
        .findById(Objects.requireNonNull(id))
        .orElseThrow(AdoptionPostException::forAdoptionPostNotExists);
  }

  public Long create(AdoptionPostCreateDto dto, Long writerId) {
    List<String> uploadTokens = Objects.requireNonNullElse(dto.getUploadTokens(), List.of());
    AdoptionPost.validateImageCount(uploadTokens.size());

    User writer = userService.findEntityById(writerId);
    AdoptionPost adoptionPost = dto.toEntity(writer);

    // 확정은 트랜잭션 밖에서 한다. 이후 어디서 실패해도 파일 행은 PENDING으로 남고 고아 정리가 지운다
    List<FileUploadDto> confirmedImages =
        storageUploadService.confirm(writerId, UploadPurpose.ADOPTION_POST_IMAGE, uploadTokens);

    return transactionTemplate.execute(
        status -> {
          adoptionPost.changeImages(toPostImages(storageFileService.attach(confirmedImages)));
          return adoptionPostRepository.save(adoptionPost).getId();
        });
  }

  public void update(Long postId, Long userId, AdoptionPostUpdateDto dto) {
    List<String> imagesToKeep = normalizeImagesToKeep(dto.getImagesToKeep());
    List<String> uploadTokens = Objects.requireNonNullElse(dto.getUploadTokens(), List.of());

    // 권한 없는 요청이나 잘못된 이미지 구성은 저장소 조회 전에 거부. 지연 로딩하는 이미지 목록을 읽으므로 트랜잭션 안에서 검사
    transactionTemplate.executeWithoutResult(
        status ->
            getUpdatablePost(postId, userId)
                .validateImageReplacement(imagesToKeep, uploadTokens.size()));

    // 1. 새 이미지 확정 (트랜잭션 외부에서 수행하여 DB 커넥션 점유 최소화)
    List<FileUploadDto> confirmedImages =
        storageUploadService.confirm(userId, UploadPurpose.ADOPTION_POST_IMAGE, uploadTokens);

    AdoptionPostEditor editor =
        AdoptionPostEditor.builder()
            .title(dto.getTitle())
            .age(dto.getAge())
            .weight(dto.getWeight())
            .features(dto.getFeatures())
            .area(dto.getArea())
            .species(dto.getSpecies())
            .sex(dto.getSex())
            .neutering(dto.getNeutering())
            .build();

    // 2. DB 트랜잭션 (데이터 변경). 실패해도 확정된 새 파일은 고아 정리가 지운다
    transactionTemplate.executeWithoutResult(
        status -> {
          // 확정하는 동안 삭제됐을 수 있으므로 트랜잭션 안에서 다시 조회하고 검사
          AdoptionPost info = getUpdatablePost(postId, userId);
          info.updateInfo(editor);

          List<String> storedFileNamesToDelete =
              info.getImages().stream()
                  .map(AdoptionPostImage::getStoredFileName)
                  .filter(fileName -> !imagesToKeep.contains(fileName))
                  .collect(Collectors.toList());

          // 이미지 교체 (유지할 것은 두고, 삭제할 것은 지우고, 새것은 추가)
          info.replaceImages(
              imagesToKeep, toPostImages(storageFileService.attach(confirmedImages)));

          // 3. 트랜잭션 안에서 이벤트 발행 -> 커밋 성공 시에만 리스너 동작
          if (!storedFileNamesToDelete.isEmpty()) {
            eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(storedFileNamesToDelete));
          }
        });
  }

  private AdoptionPost getUpdatablePost(Long postId, Long userId) {
    AdoptionPost adoptionPost = findEntityById(postId);
    if (adoptionPost.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }
    validateWriter(adoptionPost, userId);
    return adoptionPost;
  }

  private List<AdoptionPostImage> toPostImages(List<StorageFile> storageFiles) {
    return storageFiles.stream()
        .map(file -> AdoptionPostImage.builder().storageFile(file).build())
        .toList();
  }

  private List<String> normalizeImagesToKeep(List<String> imagesToKeep) {
    if (imagesToKeep == null || imagesToKeep.isEmpty()) {
      return Collections.emptyList();
    }

    return imagesToKeep.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(fileName -> !fileName.isEmpty())
        .distinct()
        .toList();
  }

  private String resolveViewUrlOrNull(String storedFileName) {
    if (storedFileName == null || storedFileName.isBlank()) {
      return null;
    }

    try {
      return storageService.getViewUrlOrNull(storedFileName);
    } catch (StorageException e) {
      if (e.getErrorSpec() == StorageError.FILE_NOT_FOUND) {
        log.warn("event=adoption_image_url_not_found storedFileName={}", storedFileName);
        return null;
      }
      throw e;
    }
  }

  // 검색
  @Transactional(readOnly = true)
  public Page<AdoptionPostDto> search(AdoptionPostSearchFilterDto dto, Pageable pageable) {

    Page<AdoptionPost> resultPage =
        adoptionPostRepository.findAll(
            Specification.allOf(
                AdoptionPostSpecification.isNotDeleted(),
                AdoptionPostSpecification.hasSpecies(dto.getSpecies()),
                AdoptionPostSpecification.hasSex(dto.getSex())),
            Objects.requireNonNull(pageable));

    return resultPage.map(
        post -> {
          AdoptionPostDto postDto = AdoptionPostDto.from(post);
          postDto.setImageUrl(resolveViewUrlOrNull(postDto.getImageUrl()));
          return postDto;
        });
  }

  @Transactional(readOnly = true)
  public Page<AdoptionPostDto> searchByWriter(Long writerId, Pageable pageable) {
    Page<AdoptionPost> resultPage =
        adoptionPostRepository.findByWriter_IdAndDeletedAtIsNull(writerId, pageable);
    return resultPage.map(
        post -> {
          AdoptionPostDto postDto = AdoptionPostDto.from(post);
          postDto.setImageUrl(resolveViewUrlOrNull(postDto.getImageUrl()));
          return postDto;
        });
  }

  @Transactional(readOnly = true)
  public Page<AdoptionPostDto> searchByAdopter(Long adopterId, Pageable pageable) {
    Page<AdoptionPost> resultPage =
        adoptionAdopterRepository.findAdoptionPostsByAdopterId(adopterId, pageable);
    return resultPage.map(
        post -> {
          AdoptionPostDto postDto = AdoptionPostDto.from(post);
          postDto.setImageUrl(resolveViewUrlOrNull(postDto.getImageUrl()));
          return postDto;
        });
  }

  @Transactional(readOnly = true)
  public AdoptionPostDto getSummary(Long id) {
    AdoptionPost info = findEntityById(id);
    AdoptionPostDto postDto = AdoptionPostDto.from(info);
    postDto.setImageUrl(resolveViewUrlOrNull(postDto.getImageUrl()));
    return postDto;
  }

  // 상세
  @Transactional(readOnly = true)
  public AdoptionPostDetailDto getDetail(Long id) {
    AdoptionPost info = findEntityById(id);
    if (info.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }

    AdoptionPostDetailDto detailResponseDto = AdoptionPostDetailDto.from(info);

    if (detailResponseDto.getWriter() != null
        && detailResponseDto.getWriter().getImageUrl() != null) {
      detailResponseDto
          .getWriter()
          .setImageUrl(resolveViewUrlOrNull(detailResponseDto.getWriter().getImageUrl()));
    }

    List<String> imageUrls =
        info.getImages().stream()
            .map(image -> resolveViewUrlOrNull(image.getStoredFileName()))
            .collect(Collectors.toList());

    detailResponseDto.setImageUrls(imageUrls);

    if (info.getStatus() == AdoptionPostStatus.COMPLETED) {
      adoptionAdopterRepository
          .findByAdoptionPostId(id)
          .ifPresent(
              adopterMapping -> {
                UserInfoDto adopterDto = UserInfoDto.from(adopterMapping.getAdopter());
                if (adopterDto.getImageUrl() != null) {
                  adopterDto.setImageUrl(resolveViewUrlOrNull(adopterDto.getImageUrl()));
                }
                detailResponseDto.setAdopter(adopterDto);
              });
    }

    detailResponseDto.setChatRoomCount(chatRoomRepository.countByAdoptionPostId(id));

    return detailResponseDto;
  }

  /**
   * AdoptiopPostStatus를 모집중 <-> 예약중 으로 변경합니다
   *
   * @param id 변경할 AdoptionPost ID
   * @param status 변경할 AdoptionPostStatus
   * @param writerId 변경할 AdoptionPost의 작성자
   */
  @Transactional
  public void updateRecruitmentStatus(Long id, AdoptionPostStatus status, Long writerId) {
    AdoptionPost info = findEntityById(id);

    if (info.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }

    // 작성자만 변경 가능
    validateWriter(info, writerId);

    // 완료 상태라면 변경 불가
    if (info.getStatus() == AdoptionPostStatus.COMPLETED) {
      throw AdoptionPostException.forAdoptionPostAlreadyCompleted();
    }

    // 완료 상태로 변경하는 책임은 가지지 않음
    if (status == AdoptionPostStatus.COMPLETED) {
      throw AdoptionPostException.forAdoptionCompletionRequired();
    }

    info.updateStatus(status);
  }

  @Transactional
  public void delete(Long id, Long userId) {
    AdoptionPost info = findEntityById(id);

    if (info.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }

    validateWriter(info, userId);

    // 성능상 트랜잭션에서 저장소에 접근하지 않고 이벤트를 호출하여 분리
    List<String> imagesToDelete = new ArrayList<>();
    if (info.getImages() != null) {
      imagesToDelete.addAll(
          info.getImages().stream()
              .map(AdoptionPostImage::getStoredFileName)
              .filter(Objects::nonNull)
              .toList());
    }

    // 댓글 삭제
    adoptionCommentService.deleteCommentsByPostId(id);
    // 게시글 삭제
    info.softDelete();
    // 채팅방 종료
    chatService.closeAllChatRoomsByPostId(id);

    if (!imagesToDelete.isEmpty()) {
      eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imagesToDelete));
    }
    auditLogger.info("event=adoption_post_deleted userId={} postId={}", userId, id);
  }

  // 탈퇴한 작성자의 게시글을 논리 삭제하고, 게시글 이미지는 커밋 후 스토리지에서 삭제
  @Transactional
  public void deleteAllByWriter(Long writerId) {
    List<String> imagesToDelete =
        adoptionPostImageRepository.findStoredFileNamesByWriter(writerId).stream()
            .filter(Objects::nonNull)
            .toList();

    adoptionPostRepository.setDeletedTimeByWriter(writerId);
    adoptionPostImageRepository.deleteImagesByWriter(writerId);

    if (!imagesToDelete.isEmpty()) {
      eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imagesToDelete));
    }
  }

  public void validateWriter(AdoptionPost info, Long userId) {
    if (!info.getWriter().getId().equals(userId)) {
      throw AuthException.forAccessDenied();
    }
  }

  /*
   * AdoptionPost와 연결된 1:1 채팅방 중 작성자가 아닌 유저들을 조회하여 반환
   */
  @Transactional(readOnly = true)
  public List<AdoptionCandidateDto> getCandidates(Long adoptionPostId, Long userId) {
    AdoptionPost adoptionPost = findEntityById(adoptionPostId);
    validateWriter(adoptionPost, userId);

    List<RoomParticipant> participants =
        participantRepository.findAllByAdoptionPostId(adoptionPostId);
    return participants.stream()
        .filter(participant -> !Objects.equals(participant.getUser().getId(), userId))
        .map(
            participant -> {
              User otherUser = participant.getUser();
              String imageUrl = null;
              if (otherUser.getImage() != null) {
                imageUrl = resolveViewUrlOrNull(otherUser.getImage().getStoredFileName());
              }
              return AdoptionCandidateDto.from(otherUser, imageUrl);
            })
        .collect(Collectors.toList());
  }

  @Transactional
  public void completeAdoption(Long id, Long adopterId, Long userId) {
    AdoptionPost info = findEntityById(id);

    if (info.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }

    validateWriter(info, userId);

    if (info.getStatus() == AdoptionPostStatus.COMPLETED) {
      throw AdoptionPostException.forAdoptionPostAlreadyCompleted();
    }

    User adopter = userService.findEntityById(adopterId);

    info.updateStatus(AdoptionPostStatus.COMPLETED);

    AdoptionAdopter adoptionAdopter =
        Objects.requireNonNull(
            AdoptionAdopter.builder().adoptionPost(info).adopter(adopter).build());
    adoptionAdopterRepository.save(adoptionAdopter);
    auditLogger.info(
        "event=adoption_completed userId={} postId={} adopterId={}", userId, id, adopterId);
  }

  @Transactional
  public void cancelAdoption(Long id, Long userId) {
    AdoptionPost info = findEntityById(id);

    if (info.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }

    validateWriter(info, userId);

    if (info.getStatus() != AdoptionPostStatus.COMPLETED) {
      throw AdoptionPostException.forAdoptionPostNotCompleted();
    }

    info.updateStatus(AdoptionPostStatus.PROCEEDING);
    adoptionAdopterRepository.deleteByAdoptionPostId(id);
    auditLogger.info("event=adoption_cancelled userId={} postId={}", userId, id);
  }
}
