package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserLoginDTO {
    @NotBlank
    @Email(message = "유효하지 않은 이메일 형태입니다.")
    private String email;

    @NotBlank
    @Size(min=8, max=20)
    private String password;
}
