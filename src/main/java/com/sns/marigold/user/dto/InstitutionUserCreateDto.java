package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitutionUserCreateDto {

  @NotBlank
  @Size(min = 1, max = 12, message = "1자 이상 12자 이하로 구성")
  private String username;

  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$", message = "영문자와 숫자, 특수문자(!@#$%^&*)를 포함한 8자 이상으로 구성.")
  private String password;

  //
  @Email
  @NotBlank
  private String email;

  @NotBlank
  private String companyName;

  @NotBlank
  private String repName; // 대표자명

  @NotBlank
  @Pattern(regexp = "^[0-9]{3}-[0-9]{2}-[0-9]{5}?")
  private String brn; // business registration number;

  @NotBlank
  @Size(min = 5, max = 5)
  private String zipCode;

  @NotBlank
  private String address;

  @NotBlank
  private String detailedAddress;
}
