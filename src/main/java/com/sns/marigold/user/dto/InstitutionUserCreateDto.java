package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class InstitutionUserCreateDto {

  @Size(min = 3, max = 12)
  private String username;

  @Email
  private String email;

  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,25}$", message = "영문자와 숫자를 포함한 8자 이상 25자 이하로 구성해야합니다.")
  private String password;

  private String contactPerson;

  @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$")
  private String contactPhone;

  private String registrationNumber;

  private String address;
}
