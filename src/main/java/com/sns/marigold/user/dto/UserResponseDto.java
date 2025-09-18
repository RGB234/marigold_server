package com.sns.marigold.user.dto;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.entity.PersonalUser;
import com.sns.marigold.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class UserResponseDto {

  private Long id;
  private Role role;
  private String username;

  private String email;
  private String contactPhone;
  private String address;


  public static UserResponseDto fromUser(User user) {
    return UserResponseDto.builder().id(user.getId()).role(user.getRole()).build();
  }

  public static UserResponseDto fromPersonalUser(PersonalUser user) {
    return UserResponseDto
      .builder()
      .id(user.getId())
      .role(user.getRole())
      .username(user.getUsername())
      .build();
  }


  public static UserResponseDto fromInstitutionUser(InstitutionUser user) {
    return UserResponseDto
      .builder()
      .id(user.getId())
      .role(user.getRole())
      .username(user.getUsername())
      .email(user.getEmail())
      .contactPhone(user.getContactPhone())
      .address(user.getAddress())
      .build();
  }
}
