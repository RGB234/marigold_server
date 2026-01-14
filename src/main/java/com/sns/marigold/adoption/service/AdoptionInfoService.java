package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionInfoUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionImage;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.entity.AdoptionInfoEditor;
import com.sns.marigold.adoption.enums.AdoptionStatus;
import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.adoption.specification.AdoptionInfoSpecification;
import com.sns.marigold.global.dto.ImageUploadDto;
import com.sns.marigold.global.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoService {
  private final UserService userService;
  private final AdoptionInfoRepository adoptionInfoRepository;
  private final S3Service s3Service;
  private final TransactionTemplate transactionTemplate;

  @Transactional(readOnly = true)
  public AdoptionInfo findEntityById(@NonNull Long id) {
    return adoptionInfoRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("AdoptionInfo not found"));
  }

  public void create(AdoptionInfoCreateDto dto, @NonNull UUID writerId) {
    List<MultipartFile> images = dto.getImages() != null ? dto.getImages() : Collections.emptyList();

    // 실패 시 예외가 발생. 업로드 된 이미지는 자동으로 삭제됨.
    List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(images);

    try {
      // 트랜잭션
      transactionTemplate.executeWithoutResult(status -> {
        saveAdoptionInfoInTransaction(dto, writerId, uploadedImages);
      });
    } catch (Exception e) {
      // DB 저장 실패 시 S3 파일 삭제 (보상 트랜잭션)
      log.error("DB Save failed. Deleting S3 files..., error: {}", e.getMessage());
      s3Service.deleteUploadedImagesFromS3(uploadedImages);
      throw e;
    }
  }

  // 실제 DB 저장은 별도 메서드로 분리하여 트랜잭션 최소화
  protected void saveAdoptionInfoInTransaction(AdoptionInfoCreateDto dto, UUID writerId,
      List<ImageUploadDto> uploadedImages) {
    User writer = userService.findEntityById(writerId);
    AdoptionInfo adoptionInfo = dto.toEntity(writer);
    Objects.requireNonNull(adoptionInfo, "adoptionInfo cannot be null");

    // 연관 관계 설정
    for (ImageUploadDto imageDto : uploadedImages) {
      adoptionInfo.addImage(AdoptionImage.builder()
          .imageUrl(imageDto.getImageUrl())
          .storeFileName(imageDto.getStoreFileName())
          .originalFileName(imageDto.getOriginalFileName())
          .build());
    }

    adoptionInfoRepository.save(adoptionInfo);
  }

  // 검색
  @Transactional(readOnly = true)
  public Page<AdoptionInfoResponseDto> search(AdoptionInfoSearchFilterDto dto, @NonNull Pageable pageable) {

    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findAll(
        Specification.allOf(
            AdoptionInfoSpecification.hasSpecies(dto.getSpecies()),
            AdoptionInfoSpecification.hasSex(dto.getSex())),
        pageable);

    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 상세
  @Transactional(readOnly = true)
  public AdoptionDetailResponseDto getDetail(@NonNull Long id) {
    AdoptionInfo info = findEntityById(id);
    return AdoptionDetailResponseDto.from(info);
  }

  @Transactional
  public void update(User user, AdoptionInfoUpdateDto dto, @NonNull Long id) {
    AdoptionInfo info = findEntityById(id);

    validateWriter(info, user);
    validateStatus(info);

    AdoptionInfoEditor editor = AdoptionInfoEditor.builder()
        .name(dto.getName())
        .age(dto.getAge())
        .weight(dto.getWeight())
        .features(dto.getFeatures())
        .area(dto.getArea())
        .species(dto.getSpecies())
        .sex(dto.getSex())
        .neutering(dto.getNeutering())
        .build();

    info.updateInfo(editor);
  }

  public void changeImages(User user, List<MultipartFile> images, @NonNull Long id) {
    AdoptionInfo info = findEntityById(id);

    validateWriter(info, user);
    validateStatus(info);

    List<ImageUploadDto> oldImages = info.getImages().stream().map(ImageUploadDto::from).collect(Collectors.toList());

    List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(images);
    List<AdoptionImage> newImages = uploadedImages.stream().map(image -> AdoptionImage.builder()
        .imageUrl(image.getImageUrl())
        .storeFileName(image.getStoreFileName())
        .originalFileName(image.getOriginalFileName())
        .build()).collect(Collectors.toList());

    // transaction
    try {
      transactionTemplate.executeWithoutResult(status -> {
        info.changeImages(newImages);
      });
      s3Service.deleteUploadedImagesFromS3(oldImages);
    } catch (Exception e) {
      s3Service.deleteUploadedImagesFromS3(uploadedImages);
      throw e;
    }
  }

  @Transactional
  public void updateStatus(User adopter, @NonNull Long id, @NonNull AdoptionStatus status) {
    AdoptionInfo info = findEntityById(id);

    validateWriter(info, adopter);
    validateStatus(info);

    if (status == AdoptionStatus.COMPLETED) {
      // Do nothing
    } else if (status == AdoptionStatus.RECRUITING) {
      info.completeAdoption(adopter);
    } else {
      throw new IllegalArgumentException("Invalid status");
    }
  }

  public void delete(User user, @NonNull Long id) {
    List<ImageUploadDto> images = null;
    images = transactionTemplate.execute(status -> {
      AdoptionInfo info = findEntityById(id);
      validateWriter(info, user);
      validateStatus(info);

      // 삭제 전 이미지 리스트 추출 (영속성 컨텍스트가 살아있을 때 수행)
      List<ImageUploadDto> dtoList = info.getImages() != null
          ? info.getImages().stream().map(ImageUploadDto::from).collect(Collectors.toList())
          : Collections.emptyList();

      return dtoList;
    });
    s3Service.deleteUploadedImagesFromS3(images);
  }

  private void validateWriter(AdoptionInfo info, User user) {
    if (!info.getWriter().getId().equals(user.getId())) {
      throw new AccessDeniedException("수정 권한이 없습니다.");
    }
    ;
  }

  // 입양 완료된 게시글은 수정 불가
  private void validateStatus(AdoptionInfo info) {
    if (info.getStatus() == AdoptionStatus.COMPLETED) {
      throw new IllegalArgumentException("Completed adoption cannot be modified");
    }
  }
}