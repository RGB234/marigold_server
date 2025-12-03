package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailedInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.adoption.specification.AdoptionInfoSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoService {

  private final AdoptionInfoRepository adoptionInfoRepository;

  @Transactional
  public void createInfo(AdoptionInfoCreateDto dto,
      UUID uid) {
    AdoptionInfo adoptionInfo = dto.toEntity(uid);
    Objects.requireNonNull(adoptionInfo, "AdoptionInfo cannot be null");
    adoptionInfoRepository.save(adoptionInfo);
  }

  // 전체 조회
  public Page<AdoptionInfoResponseDto> getAll(int page, int size) {
    // 페이지 요청 객체 생성 (page는 0부터 시작, size는 가져올 개수)
    // 최신순 정렬(Sort.Direction.DESC, "id")
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findAll(pageable);

    // Entity -> Dto 변환 (Page 인터페이스가 map 함수를 지원합니다)
    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 검색
  public Page<AdoptionInfoResponseDto> search(AdoptionInfoSearchFilterDto dto, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findAll(
        Specification.allOf(
            AdoptionInfoSpecification.hasSpecies(dto.getSpecies()),
            AdoptionInfoSpecification.hasSex(dto.getSex())),
        pageable);

    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 작성자 검색
  public Page<AdoptionInfoResponseDto> searchByWriterId(UUID uid, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<AdoptionInfo> resultPage = adoptionInfoRepository.findAllByWriterId(
        uid, pageable);
    return resultPage.map(AdoptionInfoResponseDto::from);
  }

  // 상세
  public AdoptionDetailedInfoResponseDto getDetail(UUID id) {
    AdoptionInfo info = adoptionInfoRepository.findById(id);
    return AdoptionDetailedInfoResponseDto.from(info);
  }
}