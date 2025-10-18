package com.sns.marigold.user.service;

import com.sns.marigold.auth.oauth2.RandomUsernameGenerator;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.PersonalUserCreateDto;
import com.sns.marigold.user.dto.PersonalUserResponseDto;
import com.sns.marigold.user.dto.PersonalUserUpdateDto;
import com.sns.marigold.user.dto.UserResponseDto;
import com.sns.marigold.user.entity.PersonalUser;
import com.sns.marigold.user.repository.PersonalUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalUserService implements UserService {

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
    String nickname = randomUsernameGenerator.generate();

    PersonalUser user = PersonalUser.builder()
        .providerInfo(dto.getProviderInfo())
        .providerId(dto.getProviderId())
        .nickname(nickname)
        .build();

    personalUserRepository.save(user);
//        return PersonalUserResponseDto.fromUser(user);
    return user.getId();
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

  public List<PersonalUserResponseDto> getByNickname(String nickname) {
    List<PersonalUser> users = personalUserRepository.findByNickname(nickname);
    return users.stream().map(PersonalUserResponseDto::fromUser).toList();
  }

  // ** Delete **

  public void delete(UUID id) {
    if (!personalUserRepository.existsById(id)) {
      throw new EntityNotFoundException("해당 사용자를 찾을 수 없습니다");
    }
    personalUserRepository.deleteById(id);
  }

  @Override
  public UserResponseDto loadUserById(UUID id) throws UsernameNotFoundException {
    PersonalUser user = personalUserRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("해당 사용자를 찾을 수 없습니다"));
    return new UserResponseDto(PersonalUserResponseDto.fromUser(user), null, null);
  }

  @Override
  public Role getRole() {
    return Role.ROLE_PERSON;
  }
}
