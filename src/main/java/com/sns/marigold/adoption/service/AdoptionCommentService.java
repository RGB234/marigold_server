package com.sns.marigold.adoption.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.adoption.dto.AdoptionCommentCreateDto;
import com.sns.marigold.adoption.dto.AdoptionCommentResponseDto;
import com.sns.marigold.adoption.dto.AdoptionCommentUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionComment;
import com.sns.marigold.adoption.entity.AdoptionCommentImage;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.exception.AdoptionCommentException;
import com.sns.marigold.adoption.exception.AdoptionPostException;
import com.sns.marigold.adoption.repository.AdoptionAdopterRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentImageRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.service.StorageService;
import com.sns.marigold.storage.service.StorageUploadService;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdoptionCommentService {

  private final AdoptionCommentRepository adoptionCommentRepository;
  private final AdoptionPostRepository adoptionPostRepository;
  private final AdoptionAdopterRepository adoptionAdopterRepository;
  private final UserService userService;
  private final StorageService storageService;
  private final StorageUploadService storageUploadService;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;
  private final AdoptionCommentImageRepository adoptionCommentImageRepository;

  @Transactional(readOnly = true)
  public AdoptionComment findEntityById(Long id) {
    return adoptionCommentRepository
        .findById(Objects.requireNonNull(id))
        .orElseThrow(AdoptionCommentException::forAdoptionCommentNotFound);
  }

  public Long createComment(Long postId, Long userId, AdoptionCommentCreateDto dto) {
    List<String> uploadTokens = Objects.requireNonNullElse(dto.getUploadTokens(), List.of());
    AdoptionComment.validateImageCount(uploadTokens.size());

    AdoptionPost adoptionPost =
        adoptionPostRepository
            .findById(postId)
            .orElseThrow(AdoptionPostException::forAdoptionPostNotExists);

    if (adoptionPost.getDeletedAt() != null) {
      throw AdoptionPostException.forAdoptionPostDeleted();
    }

    // 권한 검증: 게시글 작성자이거나 입양자여야 함
    boolean isWriter = adoptionPost.getWriter().getId().equals(userId);
    boolean isAdopter =
        adoptionAdopterRepository.existsByAdoptionPostIdAndAdopterId(postId, userId);

    if (!isWriter && !isAdopter) {
      throw AuthException.forAccessDenied();
    }

    User writer = userService.findEntityById(userId);

    AdoptionComment parent = null;
    if (dto.getParentId() != null) {
      parent = findEntityById(dto.getParentId());
      if (!parent.getAdoptionPost().getId().equals(postId)) {
        throw AdoptionCommentException.forAdoptionCommentPostMismatch();
      }
    }

    // 확정은 트랜잭션 밖에서 한다. 저장이 실패해도 확정된 파일은 고아 정리가 지운다
    List<FileUploadDto> confirmedImages =
        storageUploadService.confirm(userId, UploadPurpose.ADOPTION_COMMENT_IMAGE, uploadTokens);

    final AdoptionComment finalParent = parent;

    return transactionTemplate.execute(
        status -> {
          AdoptionComment comment =
              AdoptionComment.builder()
                  .adoptionPost(adoptionPost)
                  .writer(writer)
                  .parent(finalParent)
                  .content(dto.getContent())
                  .build();

          for (AdoptionCommentImage image : toCommentImages(confirmedImages)) {
            comment.addImage(image);
          }

          return adoptionCommentRepository.save(comment).getId();
        });
  }

  public void updateComment(
      Long postId, Long commentId, Long userId, AdoptionCommentUpdateDto dto) {
    List<String> uploadTokens = Objects.requireNonNullElse(dto.getUploadTokens(), List.of());
    if (dto.shouldRemoveImage() && !uploadTokens.isEmpty()) {
      throw AdoptionCommentException.forInvalidCommentImages();
    }

    if (!uploadTokens.isEmpty()) {
      AdoptionComment.validateImageCount(uploadTokens.size());
      transactionTemplate.executeWithoutResult(
          status -> getUpdatableComment(postId, commentId, userId));
    } else if (dto.shouldRemoveImage()) {
      AdoptionComment.validateImageCount(0);
    }

    List<FileUploadDto> confirmedImages =
        storageUploadService.confirm(userId, UploadPurpose.ADOPTION_COMMENT_IMAGE, uploadTokens);

    transactionTemplate.executeWithoutResult(
        status -> {
          AdoptionComment comment = getUpdatableComment(postId, commentId, userId);
          if (uploadTokens.isEmpty() && !dto.shouldRemoveImage()) {
            AdoptionComment.validateImageCount(comment.getImages().size());
          }

          comment.update(dto.getContent());

          if (!confirmedImages.isEmpty() || dto.shouldRemoveImage()) {
            List<String> imagesToDelete =
                comment.getImages().stream()
                    .map(AdoptionCommentImage::getStoredFileName)
                    .collect(Collectors.toList());

            comment.changeImages(toCommentImages(confirmedImages));

            if (!imagesToDelete.isEmpty()) {
              eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imagesToDelete));
            }
          }
        });
  }

  private List<AdoptionCommentImage> toCommentImages(List<FileUploadDto> confirmedImages) {
    return confirmedImages.stream()
        .map(
            image ->
                AdoptionCommentImage.builder()
                    .storedFileName(image.getStoredFileName())
                    .originalFileName(image.getOriginalFileName())
                    .build())
        .collect(Collectors.toList());
  }

  private AdoptionComment getUpdatableComment(Long postId, Long commentId, Long userId) {
    AdoptionComment comment = findEntityById(commentId);

    if (comment.getDeletedAt() != null) {
      throw AdoptionCommentException.forAdoptionCommentDeleted();
    }

    if (!comment.getAdoptionPost().getId().equals(postId)) {
      throw AdoptionCommentException.forAdoptionCommentPostMismatch();
    }

    if (!comment.getWriter().getId().equals(userId)) {
      throw AuthException.forAccessDenied();
    }

    return comment;
  }

  @Transactional(readOnly = true)
  public List<AdoptionCommentResponseDto> getComments(Long postId) {
    List<AdoptionComment> comments =
        adoptionCommentRepository.findByAdoptionPostIdOrderByCreatedAtAsc(postId);
    Map<Long, AdoptionCommentResponseDto> dtoMap = new HashMap<>();
    List<AdoptionCommentResponseDto> rootComments = new ArrayList<>();

    // 작성일이 빠른 순서부터 댓글 목록 조회
    for (AdoptionComment comment : comments) {
      List<String> imageUrls =
          comment.getImages().stream()
              .map(img -> storageService.getViewUrlOrNull(img.getStoredFileName()))
              .collect(Collectors.toList());

      AdoptionCommentResponseDto dto =
          AdoptionCommentResponseDto.from(comment, imageUrls, new ArrayList<>());

      if (dto.getWriter() != null && dto.getWriter().getImageUrl() != null) {
        dto.getWriter().setImageUrl(storageService.getViewUrlOrNull(dto.getWriter().getImageUrl()));
      }

      dtoMap.put(dto.getId(), dto);

      if (comment.getParent() == null) {
        rootComments.add(dto);
      } else {
        AdoptionCommentResponseDto parentDto = dtoMap.get(comment.getParent().getId());
        if (parentDto != null) {
          parentDto.getChildren().add(dto);
        }
      }
    }
    return rootComments;
  }

  public void deleteComment(Long commentId, Long userId) {
    transactionTemplate.executeWithoutResult(
        status -> {
          AdoptionComment comment = findEntityById(commentId);

          if (comment.getDeletedAt() != null) {
            throw AdoptionCommentException.forAdoptionCommentDeleted();
          }

          if (!comment.getWriter().getId().equals(userId)) {
            throw AuthException.forAccessDenied();
          }

          List<String> imagesToDelete =
              comment.getImages().stream()
                  .map(AdoptionCommentImage::getStoredFileName)
                  .collect(Collectors.toList());

          comment.softDelete();
          comment.getImages().clear();

          if (!imagesToDelete.isEmpty()) {
            // 삭제된 댓글의 이미지는 커밋 이후 스토리지에서도 삭제
            eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imagesToDelete));
          }
        });
  }

  @Transactional
  public void deleteCommentsByPostId(Long postId) {
    List<String> imagesToDelete =
        adoptionCommentImageRepository.findStoredFileNamesByAdoptionPostId(postId);

    adoptionCommentImageRepository.deleteByAdoptionPostId(postId);
    adoptionCommentRepository.clearParentByAdoptionPostId(
        postId); // self FK 끊기 - 삭제 시 FK 로 인한 삭제 실패 방지
    adoptionCommentRepository.deleteByAdoptionPostId(postId);

    if (imagesToDelete != null) {
      eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imagesToDelete));
    }
  }
}
