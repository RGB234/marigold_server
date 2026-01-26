package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionInfoUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionImage;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.entity.AdoptionInfoEditor;
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

import java.util.ArrayList;
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

  public Long create(AdoptionInfoCreateDto dto, @NonNull UUID writerId) {
    List<MultipartFile> images = dto.getImages() != null ? dto.getImages() : Collections.emptyList();
    User writer = userService.findEntityById(writerId);
    AdoptionInfo adoptionInfo = dto.toEntity(writer);

    // 실패 시 예외가 발생. 업로드 된 이미지는 자동으로 삭제됨.
    List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(images);

    try {
      // 트랜잭션
      Long adoptionInfoId = transactionTemplate.execute(status -> {
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
        return adoptionInfo.getId();

      });
      return adoptionInfoId;

    } catch (Exception e) {
      // DB 저장 실패 시 S3 파일 삭제 (보상 트랜잭션)
      log.error("DB Save failed. Deleting S3 files..., error: {}", e.getMessage());
      s3Service.deleteUploadedImagesFromS3(uploadedImages);
      throw e;
    }
  }

  /*
   * 이미지와 status 값을 제외한 필드들을 업데이트
   */
  public void update(@NonNull Long postId, @NonNull UUID userId, @NonNull AdoptionInfoUpdateDto dto) {
    AdoptionInfo infoForValidation = findEntityById(postId);

    validateWriter(infoForValidation, userId);
    validateStatus(infoForValidation);
    // 업로드 중 실패시 예외처리하여 업르드 된 파일 삭제
    // 이미지 없을 시 empty list 반환
    final List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(dto.getImages());
    List<ImageUploadDto> oldImages = new ArrayList<>();

    AdoptionInfoEditor editor = AdoptionInfoEditor.builder()
        .title(dto.getTitle())
        .age(dto.getAge())
        .weight(dto.getWeight())
        .features(dto.getFeatures())
        .area(dto.getArea())
        .species(dto.getSpecies())
        .sex(dto.getSex())
        .neutering(dto.getNeutering())
        .build();

    try {
      transactionTemplate.executeWithoutResult(status -> {
        // 영속성 컨텍스트
        AdoptionInfo info = findEntityById(postId);
        info.updateInfo(editor);
        // 새 이미지가 있을 경우에만
        if (uploadedImages != null && !uploadedImages.isEmpty()) {
          // 이전 이미지 저장
          oldImages.addAll(info.getImages().stream().map(ImageUploadDto::from).collect(Collectors.toList()));
          // 덮어쓰기
          info.getImages().clear();
          for (ImageUploadDto image : uploadedImages) {
            info.addImage(AdoptionImage.builder()
                .imageUrl(image.getImageUrl())
                .storeFileName(image.getStoreFileName())
                .originalFileName(image.getOriginalFileName())
                .build());
          }
        }
      });
      if (!oldImages.isEmpty()) {
        // 스토리지 파일 삭제
        s3Service.deleteUploadedImagesFromS3(oldImages);
      }
    } catch (Exception e) {
      // DB 저장 실패 시 S3 파일 삭제 (보상 트랜잭션)
      log.error("DB Save failed");
      if (uploadedImages != null && !uploadedImages.isEmpty()) {
        log.error("Deleting S3 files..., error: {}", e.getMessage());
        s3Service.deleteUploadedImagesFromS3(uploadedImages);
      }
      throw e;
    }
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

  @Transactional(readOnly = true)
  public Page<AdoptionInfoResponseDto> searchByWriter(UUID writerId, @NonNull Pageable pageable) {
    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findByWriter_Id(writerId, pageable);
    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 상세
  @Transactional(readOnly = true)
  public AdoptionDetailResponseDto getDetail(@NonNull Long id) {
    AdoptionInfo info = findEntityById(id);
    return AdoptionDetailResponseDto.from(info);
  }

  @Transactional
  public void completeAdoption(User adopter, @NonNull Long id) {
    AdoptionInfo info = findEntityById(id);

    validateWriter(info, adopter.getId());
    validateStatus(info);

    info.completeAdoption(adopter);
  }

  public void delete(@NonNull Long id, @NonNull UUID userId) {
    // User user = userService.findEntityById(userId);
    List<ImageUploadDto> images = null;

    images = transactionTemplate.execute(status -> {
      AdoptionInfo info = findEntityById(id);
      validateWriter(info, userId);
      validateStatus(info);

      // 삭제 전 이미지 리스트 추출 (영속성 컨텍스트가 살아있을 때 수행)
      List<ImageUploadDto> dtoList = info.getImages() != null
          ? info.getImages().stream().map(ImageUploadDto::from).collect(Collectors.toList())
          : Collections.emptyList();

      adoptionInfoRepository.delete(info);

      return dtoList;
    });

    s3Service.deleteUploadedImagesFromS3(images);
  }

  // public void deleteByWriter(UUID writerId) {
  //   Page<AdoptionInfo> adoptionInfoPage = adoptionInfoRepository.findByWriter_Id(writerId, Pageable.unpaged());
  //   adoptionInfoPage.forEach(adoptionInfo -> {
  //     if (!adoptionInfo.isCompleted()) {
  //       delete(adoptionInfo.getId(), writerId);
  //     }
  //   });
  // }

  public void validateWriter(AdoptionInfo info, UUID userId) {
    if (!info.getWriter().getId().equals(userId)) {
      throw new AccessDeniedException("수정 권한이 없습니다.");
    }
    ;
  }

  // 입양 완료된 게시글은 수정 불가
  public void validateStatus(AdoptionInfo info) {
    if (info.isCompleted()) {
      throw new IllegalArgumentException("Completed adoption cannot be modified");
    }
  }
}