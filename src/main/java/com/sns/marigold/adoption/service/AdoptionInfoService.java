package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.entity.AdoptionImage;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.adoption.specification.AdoptionInfoSpecification;
import com.sns.marigold.global.dto.ImageUploadDto;
import com.sns.marigold.global.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoService {
  private final UserServiceImpl userService;
  private final AdoptionInfoRepository adoptionInfoRepository;
  private final S3Service s3Service;

  public void create(AdoptionInfoCreateDto dto, UUID writerId) {
    // 1. 유효성 검사 (Transaction 불필요)
    Objects.requireNonNull(writerId, "writerId cannot be null");
    List<MultipartFile> images = dto.getImages() != null ? dto.getImages() : Collections.emptyList();

    // 2. S3 이미지 업로드 (Transaction 밖에서 수행!)
    // 실패 시 예외가 발생하며 create 메서드 종료 -> DB 트랜잭션 시작조차 안 함
    List<ImageUploadDto> uploadedImages = uploadImagesToS3(images);

    try {
      // 3. DB 저장 (트랜잭션 진입)
      saveAdoptionInfoInTransaction(dto, writerId, uploadedImages);
    } catch (Exception e) {
      // 4. DB 저장 실패 시 S3 파일 삭제 (보상 트랜잭션)
      // 로깅 필수
      log.error("DB Save failed. Deleting S3 files...", e);
      deleteUploadedImagesFromS3(uploadedImages);
      throw e; // 예외 다시 던짐
    }
  }

  // 별도의 헬퍼 메서드로 분리 (혹은 S3Service 내부 로직)
  private List<ImageUploadDto> uploadImagesToS3(List<MultipartFile> images) {
    List<ImageUploadDto> result = new ArrayList<>();
    try {
      for (MultipartFile image : images) {
        // uploadFile 내부에서 실패 시 예외를 던지도록 설계 권장
        result.add(s3Service.uploadFile(image));
      }
    } catch (Exception e) {
      // 업로드 중간에 실패하면, 이미 올라간 파일들 삭제 후 예외 발생
      deleteUploadedImagesFromS3(result);
      throw e;
    }
    return result;
  }

  // 실제 DB 저장은 별도 메서드로 분리하여 트랜잭션 최소화
  @Transactional
  protected void saveAdoptionInfoInTransaction(AdoptionInfoCreateDto dto, UUID writerId,
      List<ImageUploadDto> uploadedImages) {
    User writer = userService.findEntityById(writerId);
    AdoptionInfo adoptionInfo = dto.toEntity(writer);
    Objects.requireNonNull(adoptionInfo, "adoptionInfo cannot be null");

    // 업로드된 정보 매핑
    for (ImageUploadDto imageDto : uploadedImages) {
      adoptionInfo.addImage(AdoptionImage.builder()
          .imageUrl(imageDto.getImageUrl())
          .storeFileName(imageDto.getStoreFileName())
          .originalFileName(imageDto.getOriginalFileName())
          .build());
    }

    adoptionInfoRepository.save(adoptionInfo);
  }

  private void deleteUploadedImagesFromS3(List<ImageUploadDto> images) {
    // S3Service에 delete 기능 구현 필요
    for (ImageUploadDto dto : images) {
      s3Service.deleteFile(dto.getStoreFileName());
    }
  }

  // 검색
  public Page<AdoptionInfoResponseDto> search(AdoptionInfoSearchFilterDto dto, @NonNull Pageable pageable) {

    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findAll(
        Specification.allOf(
            AdoptionInfoSpecification.hasSpecies(dto.getSpecies()),
            AdoptionInfoSpecification.hasSex(dto.getSex())),
        pageable);

    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 상세
  public AdoptionDetailResponseDto getDetail(@NonNull Long id) {
    AdoptionInfo info = adoptionInfoRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("AdoptionInfo not found"));
    return AdoptionDetailResponseDto.from(info);
  }
}