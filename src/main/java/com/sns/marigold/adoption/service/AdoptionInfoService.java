package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionDetailedInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.adoption.specification.AdoptionInfoSpecification;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoService {

  private final AdoptionInfoRepository adoptionInfoRepository;
  private final UserService userService;

  @Transactional
  public void createInfo(AdoptionInfoCreateDto dto,
    UUID uid) {
    User user = userService.findById(uid);

    AdoptionInfo adoptionInfo = AdoptionInfo
      .builder()
      .writer(user)
      .species(dto.getSpecies())
      .name(dto.getName())
      .age(dto.getAge())
      .sex(dto.getSex())
      .area(dto.getArea())
      .weight(dto.getWeight())
      .neutering(dto.getNeutering())
      .features(dto.getFeatures())
      .build();

    adoptionInfoRepository.save(adoptionInfo);
  }

  public List<AdoptionInfoResponseDto> getAll() {
    List<AdoptionInfo> list = adoptionInfoRepository.findAll();
    return list.stream().map(AdoptionInfoResponseDto::from).collect(Collectors.toList());
  }

  // 일반 검색
  public List<AdoptionInfoResponseDto> search(AdoptionInfoSearchFilterDto dto) {
    List<AdoptionInfo> list = adoptionInfoRepository.findAll(
      Specification.allOf(
        AdoptionInfoSpecification.hasSpecies(dto.getSpecies()),
        AdoptionInfoSpecification.hasSex(dto.getSex())
      ));
    return list.stream().map(AdoptionInfoResponseDto::from).collect(Collectors.toList());
  }

  // 작성자로 검색
  public List<AdoptionInfoResponseDto> searchByWriterId(UUID uid) {
    List<AdoptionInfo> list = adoptionInfoRepository.findAllByWriter_Id(uid);
    return list.stream()
      .map(AdoptionInfoResponseDto::from)
      .collect(Collectors.toList());
  }

  // 상세
  public AdoptionDetailedInfoResponseDto getDetail(UUID id) {
    AdoptionInfo info = adoptionInfoRepository.findByIdWithWriter_Id(id);
    return AdoptionDetailedInfoResponseDto.from(info);
  }
}