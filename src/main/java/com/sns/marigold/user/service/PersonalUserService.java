package com.sns.marigold.user.service;

import com.sns.marigold.auth.oauth2.RandomUsernameGenerator;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.user.dto.PersonalUserCreateDto;
import com.sns.marigold.user.dto.PersonalUserResponseDto;
import com.sns.marigold.user.dto.PersonalUserUpdateDto;
import com.sns.marigold.user.entity.PersonalUser;
import com.sns.marigold.user.repository.PersonalUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalUserService {

  private final PersonalUserRepository personalUserRepository;
  private final RandomUsernameGenerator randomUsernameGenerator;

  public PersonalUser findById(UUID id) {
    return personalUserRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
  }

  public PersonalUser findByProviderInfoAndProviderId(ProviderInfo providerInfo,
    String providerId) {
    return personalUserRepository
      .findByProviderInfoAndProviderId(providerInfo, providerId)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다."));
  }

  // ** create **

  @Transactional
  public UUID create(PersonalUserCreateDto dto) {
    int maxAttempts = 3;

    for (int i = 0; i < maxAttempts; i++) {
      // 유저명은 랜덤 생성
      // 이후 설정에서 변경 가능
      String username = randomUsernameGenerator.generate();

      PersonalUser user = PersonalUser.builder()
        .providerInfo(dto.getProviderInfo())
        .providerId(dto.getProviderId())
        .username(username)
        .build();

      try {
        personalUserRepository.save(user);
//        return PersonalUserResponseDto.fromUser(user);
        return user.getId();
      } catch (DataIntegrityViolationException e) {
        log.info("DataIntegrityViolationException : {}", e.getMessage());
        log.info("Retrying... ( {} / {} )", i + 1, maxAttempts);
      }
    }
    throw new IllegalStateException("계정 생성 실패. 잠시 후 다시 시도해주십시오.");
  }

  // ** update **

  @Transactional
  public PersonalUserResponseDto update(UUID id, PersonalUserUpdateDto dto) {
    // 업데이트 된 사용자 정보 가져오기
    PersonalUser user = personalUserRepository.findById(id).orElseThrow(() ->
      new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));

    user.update(dto);

    return PersonalUserResponseDto.fromUser(user);
  }

  // ** Get **

  public PersonalUserResponseDto getById(UUID id) {
    PersonalUser user = personalUserRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
    return PersonalUserResponseDto.fromUser(user);
  }

  public PersonalUserResponseDto getByUsername(String username) {
    PersonalUser user = personalUserRepository.findByUsername(username)
      .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
    return PersonalUserResponseDto.fromUser(user);
  }

  // ** Delete **

  public void delete(UUID id) {
    if (!personalUserRepository.existsById(id)) {
      throw new EntityNotFoundException("해당 사용자를 찾을 수 없습니다");
    }
    personalUserRepository.deleteById(id);
  }
}
