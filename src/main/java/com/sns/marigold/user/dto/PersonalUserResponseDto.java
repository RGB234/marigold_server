package com.sns.marigold.user.dto;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.PersonalUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalUserResponseDto {

  private Role role;
  private String username;

  public static PersonalUserResponseDto fromUser(PersonalUser user) {
    return PersonalUserResponseDto
      .builder()
      .role(user.getRole())
      .username(user.getUsername())
      .build();
  }
}
