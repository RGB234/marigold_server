package com.sns.marigold.user.dto.create;

import com.sns.marigold.global.validation.ValidationPolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로컬 회원가입 요청")
@Getter
@NoArgsConstructor
public class LocalSignupDto {
  @Schema(description = "회원가입 이메일", example = "user@example.com")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @NotBlank(message = "이메일은 필수입니다.")
  private String email;

  @Schema(description = "회원가입 비밀번호. 8자 이상", example = "password123!", format = "password")
  @Size(min = ValidationPolicy.Password.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
  @NotBlank(message = "비밀번호는 필수입니다.")
  private String password;

  @Schema(description = "닉네임. 영문자, 한글, 숫자만 허용", example = "마리골드")
  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(
      min = ValidationPolicy.User.NICKNAME_MIN_LENGTH,
      max = ValidationPolicy.User.NICKNAME_MAX_LENGTH,
      message = "닉네임은 2자 이상 12자 이하로 구성해야 합니다.")
  @Pattern(
      regexp = ValidationPolicy.User.NICKNAME_ALLOWED_PATTERN,
      message = "닉네임은 영문자, 한글, 숫자만 사용할 수 있습니다.")
  private String nickname;
}
