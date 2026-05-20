package com.sns.marigold.user.dto.update;

import com.sns.marigold.global.validation.ValidationPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailPasswordRegisterDto {

  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @NotBlank(message = "이메일은 필수입니다.")
  private String email;

  @Size(min = ValidationPolicy.Password.MIN_LENGTH, message = "비밀번호는 8자 이상이어야 합니다.")
  @NotBlank(message = "비밀번호는 필수입니다.")
  private String password;
}
