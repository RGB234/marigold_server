package com.sns.marigold.user.dto;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.InstitutionUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class InstitutionUserResponseDto {

  private Long id;
  private Role role;
  private String username;
  private String email;
  private String contactPhone;
  private String address;

  public static InstitutionUserResponseDto fromUser(InstitutionUser user) {
    return InstitutionUserResponseDto
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
