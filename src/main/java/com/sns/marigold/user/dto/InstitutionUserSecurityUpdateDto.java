package com.sns.marigold.user.dto;

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
public class InstitutionUserSecurityUpdateDto {

  @NotBlank
  @Size(min = 1, max = 12, message = "1자 이상 12자 이하로 구성")
  private String username;

  @NotBlank
  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,25}$", message = "영문자와 숫자를 포함한 8자 이상 25자 이하로 구성해야합니다.")
  private String password;
}
