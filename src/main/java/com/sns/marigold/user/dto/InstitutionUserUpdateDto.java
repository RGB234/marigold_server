package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class InstitutionUserUpdateDto {

  @Size(min = 3, max = 12)
  private String username;

  @Email
  private String email;

  private String password;

  private String contactPerson;

  @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$")
  private String contactPhone;


  private String registrationNumber;

  private String address;
}
