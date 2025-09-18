package com.sns.marigold.adoption.service;

import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.repository.AdoptionInfoRepository;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.InstitutionUserService;
import com.sns.marigold.user.service.PersonalUserService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.SessionAttribute;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoService {

  private final AdoptionInfoRepository adoptionInfoRepository;
  private final PersonalUserService personalUserService;
  private final InstitutionUserService institutionUserService;

  @Transactional
  public void createInfo(AdoptionInfoCreateDto dto,
    @SessionAttribute("userId") String userId) {

    User user =

      log.info("user : {}", user);

    AdoptionInfo adoptionInfo = AdoptionInfo
      .builder()
      .writer(user)
      .species(dto.getSpecies())
      .name(dto.getName())
      .age(dto.getAge())
      .sex(dto.getSex())
      .location(dto.getLocation())
      .weight(dto.getWeight())
      .neutering(dto.getNeutering())
      .features(dto.getFeatures())
      .build();

    adoptionInfoRepository.save(adoptionInfo);
  }

  public List<AdoptionInfoResponseDto> getAll() {
    return adoptionInfoRepository.findAll()
      .stream()
      .map(AdoptionInfoResponseDto::fromAdoptionInfo)
      .collect(Collectors.toList());
  }
}