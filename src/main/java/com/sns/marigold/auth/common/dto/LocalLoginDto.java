package com.sns.marigold.auth.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로컬 로그인 요청")
@Getter
@NoArgsConstructor
public class LocalLoginDto {
  @Schema(description = "로그인 이메일", example = "user@example.com")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @NotBlank(message = "이메일은 필수입니다.")
  private String email;

  @Schema(description = "로그인 비밀번호", example = "password123!", format = "password")
  @NotBlank(message = "비밀번호는 필수입니다.")
  private String password;
}
