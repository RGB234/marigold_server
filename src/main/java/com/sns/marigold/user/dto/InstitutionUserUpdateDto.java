package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Email;
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

  private String companyName;

  private String repName; // 대표자명

  private String brn; // business registration number;

  private String zipCode;

  private String address;

  private String detailedAddress;
}
