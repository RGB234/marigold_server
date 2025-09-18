package com.sns.marigold.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class EmailLoginDto {

  @Email
  private String email;
  
  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,25}$", message = "영문자와 숫자를 포함한 8자 이상 25자 이하로 구성해야합니다.")
  private String password;
}
