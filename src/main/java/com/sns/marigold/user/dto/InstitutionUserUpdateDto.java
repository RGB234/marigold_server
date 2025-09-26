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
public class InstitutionUserUpdateDto {

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
