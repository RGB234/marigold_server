package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailedInfoResponseDto;
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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoService {
  private final UserServiceImpl userService;
  private final AdoptionInfoRepository adoptionInfoRepository;
  private final S3Service s3Service;
  
  @Transactional
  public void create(AdoptionInfoCreateDto dto, UUID writerId) {
    Objects.requireNonNull(writerId, "writerId cannot be null");

    ImageUploadDto imageDto = s3Service.uploadFile(dto.getImage());
    User writer = userService.findEntityById(writerId);
    
    AdoptionInfo adoptionInfo = dto.toEntity(writer);
    Objects.requireNonNull(adoptionInfo, "AdoptionInfo cannot be null");

    if (imageDto != null) {
        adoptionInfo.addImage(AdoptionImage.builder()
                .imageUrl(imageDto.getImageUrl())
                .storeFileName(imageDto.getStoreFileName())
                .originalFileName(imageDto.getOriginalFileName())
                .build());
    }

    adoptionInfoRepository.save(adoptionInfo);
  }

  // 검색
  public Page<AdoptionInfoResponseDto> search(AdoptionInfoSearchFilterDto dto, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findAll(
        Specification.allOf(
            AdoptionInfoSpecification.hasWriterId(dto.getWriterId()),
            AdoptionInfoSpecification.hasSpecies(dto.getSpecies()),
            AdoptionInfoSpecification.hasSex(dto.getSex())),
        pageable);

    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 상세
  public AdoptionDetailedInfoResponseDto getDetail(UUID id) {
    AdoptionInfo info = adoptionInfoRepository.findById(id);
    return AdoptionDetailedInfoResponseDto.from(info);
  }
}