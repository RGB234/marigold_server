package com.sns.marigold.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponseDto {
  private PersonalUserResponseDto personalUserResponseDto;
  private InstitutionUserResponseDto institutionUserResponseDto;
  private AdminUserResponseDto adminUserResponseDto;

  public static UserResponseDto fromPersonalUser(PersonalUserResponseDto dto) {
    return new UserResponseDto(dto, null, null);
  }

  public static UserResponseDto fromInstitutionUser(InstitutionUserResponseDto dto) {
    return new UserResponseDto(null, dto, null);
  }

  public static UserResponseDto fromAdminUser(AdminUserResponseDto dto) {
    return new UserResponseDto(null, null, dto);
  }
}
