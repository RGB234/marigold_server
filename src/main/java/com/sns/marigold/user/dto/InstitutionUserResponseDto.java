package com.sns.marigold.user.dto;

import com.sns.marigold.user.entity.InstitutionUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitutionUserResponseDto {

  private String email;
  private String companyName;
  private String repName;
  private String brn;
  private String zipCode;
  private String address;
  private String detailedAddress;

  public static InstitutionUserResponseDto fromUser(InstitutionUser user) {
    return InstitutionUserResponseDto
      .builder()
      .email(user.getEmail())
      .companyName(user.getCompanyName())
      .repName(user.getRepName())
      .brn(user.getBrn())
      .zipCode(user.getZipCode())
      .address(user.getAddress())
      .detailedAddress(user.getDetailedAddress())
      .build();
  }
}
