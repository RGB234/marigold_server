package com.sns.marigold.user.dto.create;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LocalSignupDto {
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @NotBlank(message = "이메일은 필수입니다.")
  private String email;

  @Pattern(
      regexp = "/^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$/",
      message = "비밀번호 영어 대소문자, 숫자 조합 8자 이상이여야 합니다.")
  @NotBlank(message = "비밀번호는 필수입니다.")
  private String password;

  @NotBlank(message = "닉네임은 필수입니다.")
  @Pattern(regexp = "/^[a-zA-Z0-9가-힣]{3,12}$/", message = "영문, 한글, 숫자로 3자 이상, 12자 이하로 구성해주세요.")
  private String nickname;
}
