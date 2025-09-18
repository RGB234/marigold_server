package com.sns.marigold.user.dto;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.PersonalUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class PersonalUserResponseDto {

  private Long id;
  private Role role;
  private String username;

  public static PersonalUserResponseDto fromUser(PersonalUser user) {
    return PersonalUserResponseDto
      .builder()
      .id(user.getId())
      .role(user.getRole())
      .username(user.getUsername())
      .build();
  }
}
