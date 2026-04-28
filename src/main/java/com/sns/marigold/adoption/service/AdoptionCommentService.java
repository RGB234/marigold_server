package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionCommentCreateDto;
import com.sns.marigold.adoption.dto.AdoptionCommentResponseDto;
import com.sns.marigold.adoption.entity.AdoptionComment;
import com.sns.marigold.adoption.entity.AdoptionCommentImage;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.exception.AdoptionCommentException;
import com.sns.marigold.adoption.exception.AdoptionPostException;
import com.sns.marigold.adoption.repository.AdoptionAdopterRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.error.exception.InternalServerException;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionCommentService {

  private final AdoptionCommentRepository adoptionCommentRepository;
  private final AdoptionPostRepository adoptionPostRepository;
  private final AdoptionAdopterRepository adoptionAdopterRepository;
  private final UserService userService;
  private final S3Service s3Service;
  private final TransactionTemplate transactionTemplate;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public AdoptionComment findEntityById(Long id) {
    return adoptionCommentRepository
        .findById(Objects.requireNonNull(id))
        .orElseThrow(AdoptionCommentException::forAdoptionCommentNotFound);
  }

  public Long createComment(Long postId, Long userId, AdoptionCommentCreateDto dto) {
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

    List<MultipartFile> images =
        dto.getImages() != null ? dto.getImages() : Collections.emptyList();
    List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(images);

    final AdoptionComment finalParent = parent;

    try {
      return transactionTemplate.execute(
          status -> {
            AdoptionComment comment =
                AdoptionComment.builder()
                    .adoptionPost(adoptionPost)
                    .writer(writer)
                    .parent(finalParent)
                    .content(dto.getContent())
                    .build();

            List<AdoptionCommentImage> newImages =
                uploadedImages.stream()
                    .map(
                        image ->
                            AdoptionCommentImage.builder()
                                .storedFileName(image.getStoredFileName())
                                .originalFileName(image.getOriginalFileName())
                                .build())
                    .collect(Collectors.toList());

            for (AdoptionCommentImage image : newImages) {
              comment.addImage(image);
            }

            return adoptionCommentRepository.save(comment).getId();
          });
    } catch (Exception e) {
      log.error("Comment creation failed. Deleting uploaded S3 files... error: {}", e.getMessage());
      try {
        s3Service.deleteUploadedImagesFromS3(uploadedImages);
      } catch (Exception s3Ex) {
        log.error("Failed to delete S3 images during rollback. Files: {}", uploadedImages, s3Ex);
      }
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw InternalServerException.forInternalServerError(e);
    }
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
              .map(img -> s3Service.getPresignedGetObject(img.getStoredFileName()))
              .collect(Collectors.toList());

      AdoptionCommentResponseDto dto =
          AdoptionCommentResponseDto.from(comment, imageUrls, new ArrayList<>());

      if (dto.getWriter() != null && dto.getWriter().getImageUrl() != null) {
        dto.getWriter().setImageUrl(s3Service.getPresignedGetObject(dto.getWriter().getImageUrl()));
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
    List<String> imagesToDelete =
        transactionTemplate.execute(
            status -> {
              AdoptionComment comment = findEntityById(commentId);

              if (comment.getDeletedAt() != null) {
                throw AdoptionCommentException.forAdoptionCommentDeleted();
              }

              if (!comment.getWriter().getId().equals(userId)) {
                throw AuthException.forAccessDenied();
              }

              List<String> dtoList =
                  comment.getImages().stream()
                      .map(AdoptionCommentImage::getStoredFileName)
                      .collect(Collectors.toList());

              comment.softDelete();
              comment.getImages().clear();

              return dtoList;
            });

    if (imagesToDelete != null && !imagesToDelete.isEmpty()) {
      // 삭제된 댓글의 이미지는 S3에서도 삭제
      eventPublisher.publishEvent(new DeleteOldStorageFilesEvent(imagesToDelete));
    }
  }
}
