package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionInfoUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionImage;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.entity.AdoptionInfoEditor;
import com.sns.marigold.adoption.exception.AdoptionException;
import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.adoption.specification.AdoptionInfoSpecification;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.error.exception.BusinessException;
import com.sns.marigold.global.error.exception.InternalServerException;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
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
        .orElseThrow(AdoptionException::forAdoptionInfoNotExists);
  }

  public Long create(AdoptionInfoCreateDto dto, @NonNull Long writerId) {
    List<MultipartFile> images = dto.getImages() != null ? dto.getImages() : Collections.emptyList();
    List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(images);

    User writer = userService.findEntityById(writerId);
    AdoptionInfo adoptionInfo = dto.toEntity(writer);

    try {
      return transactionTemplate.execute(status -> {
        adoptionInfo.changeImages(uploadedImages.stream().map(image -> AdoptionImage.builder()
            .storedFileName(image.getStoredFileName())
            .originalFileName(image.getOriginalFileName())
            .build()).collect(Collectors.toList()));

        return adoptionInfoRepository.save(adoptionInfo).getId();
      });

    } catch (Exception e) {
      log.error("Create failed. Deleting uploaded S3 files... error: {}", e.getMessage());

      try {
        s3Service.deleteUploadedImagesFromS3(uploadedImages);
      } catch (Exception s3Ex) {
        log.error("Failed to delete S3 images during rollback. Files: {}", uploadedImages, s3Ex);
      }

      if (e instanceof BusinessException) {
        throw e;
      }
      throw InternalServerException.forInternalServerError(e);
    }
  }

  /*
   * 이미지와 status 값을 제외한 필드들을 업데이트
   */
  public void update(@NonNull Long postId, @NonNull Long userId, @NonNull AdoptionInfoUpdateDto dto) {
    AdoptionInfo adoptionInfo = findEntityById(postId);

    validateWriter(adoptionInfo, userId);
    validateStatus(adoptionInfo);

    // 1. 새 이미지 S3 업로드 (실패 시 예외 발생, 파일 자동 삭제됨)
    final List<ImageUploadDto> uploadedImages = s3Service.uploadImagesToS3(dto.getImages());

    // 2. 유지할 이미지 파일명 목록 (Null Safe)
    List<String> imagesToKeep = dto.getImagesToKeep() != null ? dto.getImagesToKeep() : Collections.emptyList();

    // 3. S3에서 삭제할 기존 이미지 식별
    // (현재 DB 이미지 중, 유지할 목록에 없는 것들)
    List<AdoptionImage> imagesToDeleteFromS3 = adoptionInfo.getImages().stream()
        .filter(image -> !imagesToKeep.contains(image.getStoredFileName()))
        .toList();

    // 4. 새로 추가할 이미지 엔티티 생성
    List<AdoptionImage> newImages = uploadedImages.stream()
        .map(image -> AdoptionImage.builder()
            .storedFileName(image.getStoredFileName())
            .originalFileName(image.getOriginalFileName())
            .build())
        .toList();

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
      // 5. DB 트랜잭션 (데이터 변경)
      transactionTemplate.executeWithoutResult(status -> {
        // 영속성 컨텍스트 내에서 엔티티 재조회 (필수)
        AdoptionInfo info = findEntityById(postId);
        info.updateInfo(editor);

        // 이미지 교체 (유지할 것은 두고, 삭제할 것은 지우고, 새것은 추가)
        info.replaceImages(imagesToKeep, newImages);
      });

      // 6. 트랜잭션 성공 시: S3에서 기존 파일 삭제
      if (!imagesToDeleteFromS3.isEmpty()) {
        s3Service.deleteUploadedImagesFromS3ByStoredFileNames(
            imagesToDeleteFromS3.stream()
                .map(AdoptionImage::getStoredFileName)
                .collect(Collectors.toList()));
      }

    } catch (Exception e) {
      // 7. 실패 시 보상 트랜잭션: 새로 업로드한 S3 파일 삭제
      log.error("Update failed. Deleting uploaded S3 files... error: {}", e.getMessage());

      try {
        s3Service.deleteUploadedImagesFromS3(uploadedImages);
      } catch (Exception s3Ex) {
        log.error("Failed to delete S3 images during rollback. Files: {}", uploadedImages, s3Ex);
      }

      if (e instanceof BusinessException) {
        throw e;
      }
      throw InternalServerException.forInternalServerError(e);
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
  public Page<AdoptionInfoResponseDto> searchByWriter(Long writerId, @NonNull Pageable pageable) {
    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findByWriter_Id(writerId, pageable);
    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 상세
  @Transactional(readOnly = true)
  public AdoptionDetailResponseDto getDetail(@NonNull Long id) {
    AdoptionInfo info = findEntityById(id);
    AdoptionDetailResponseDto detailResponseDto = AdoptionDetailResponseDto.from(info);

    List<String> imageUrls = info.getImages().stream()
        .map(image -> s3Service.getPresignedGetUrl(image.getStoredFileName()))
        .collect(Collectors.toList());
        
    detailResponseDto.setImageUrls(imageUrls);

    return detailResponseDto;
  }

  @Transactional
  public void completeAdoption(User adopter, @NonNull Long id) {
    AdoptionInfo info = findEntityById(id);

    validateWriter(info, adopter.getId());
    validateStatus(info);

    info.completeAdoption(adopter);
  }

  public void delete(@NonNull Long id, @NonNull Long userId) {
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

  public void validateWriter(AdoptionInfo info, Long userId) {
    if (!info.getWriter().getId().equals(userId)) {
      throw AuthException.forAuthorizationDenied();
    }
  }

  // 입양 완료된 게시글은 수정 불가
  public void validateStatus(AdoptionInfo info) {
    if (info.isCompleted()) {
      throw AdoptionException.forAdoptionInfoCompleted();
    }
  }
}
